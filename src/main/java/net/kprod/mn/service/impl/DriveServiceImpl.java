package net.kprod.mn.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import net.kprod.mn.data.gdrive.GDriveJpaDataStoreFactory;
import net.kprod.mn.data.repository.RepositoryGDriveCredential;
import net.kprod.mn.service.AuthService;
import net.kprod.mn.service.DriveService;
import net.kprod.mn.tasks.RefreshTokenTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DriveServiceImpl implements DriveService {
    private Logger LOG = LoggerFactory.getLogger(DriveServiceImpl.class);
    private static final String APPLICATION_NAME = "Drive Notepad Sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${app.drive.auth.client-id}")
    private String CLIENT_ID;

    @Value("${app.drive.auth.client-secret}")
    private String CLIENT_SECRET;

    @Value("${app.url.self}")
    private String appHost;

    @Value("${app.oauth-callback.path}")
    private String oauthCallbackPath;

    @Value("${app.drive.auth.refresh-token}")
    private long tokenRefreshInterval;

    private static final Set<String> SCOPES = Set.of(
            DriveScopes.DRIVE,
            DriveScopes.DRIVE_READONLY,
            DriveScopes.DRIVE_FILE,
            DriveScopes.DRIVE_METADATA,
            DriveScopes.DRIVE_METADATA_READONLY);

    private GoogleAuthorizationCodeFlow authFlow;
    private NetHttpTransport HTTP_TRANSPORT;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private RepositoryGDriveCredential credentialRepository;

    @Autowired
    private AuthService authService;

    private Map<String, Credential> mapCredentials = new HashMap<>();

    private Map<String, Drive> mapDrive = new HashMap<>();

    private Credential getCredential(){
        //LOG.info("Get credential user {}", authService.getUsernameFromContext());
        return mapCredentials.get(authService.getUsernameFromContext());
    }

    private Credential setCredential(Credential credential){
        //LOG.info("Set credential user {}", authService.getUsernameFromContext());
        mapCredentials.put(authService.getUsernameFromContext(), credential);
        return credential;
    }

    private Drive mapGetDrive(){
        //LOG.info("Get drive user {}", authService.getUsernameFromContext());

        if(mapDrive == null || mapDrive.containsKey(authService.getUsernameFromContext()) == false) {
            //HUHO
            //LOG.warn("WAAAAT CAN I DO");
            requireAuth();
        }

        return mapDrive.get(authService.getUsernameFromContext());
    }

    private Drive mapSetDrive(Drive drive){
        LOG.info("Set drive user {}", authService.getUsernameFromContext());
        mapDrive.put(authService.getUsernameFromContext(), drive);
        return drive;
    }

    @Override
    public Optional<String> requireAuth() {
        HttpTransport httpTransport = new NetHttpTransport();
        GDriveJpaDataStoreFactory dataStoreFactory = new GDriveJpaDataStoreFactory(credentialRepository);
        //request auth
        try {
            authFlow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPES)
                    .setDataStoreFactory(dataStoreFactory)
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Credential credential = setCredential(authFlow.loadCredential(authService.getUsernameFromContext()));

            if (credential != null && credential.getExpirationTimeMilliseconds() != null) {

                long currentTime = System.currentTimeMillis();
                long expTk = credential != null ? credential.getExpirationTimeMilliseconds() : 0;

                if(expTk <= currentTime) {
                    LOG.warn("token expired");
                    //todo not working
                    this.refreshToken();
                    currentTime = System.currentTimeMillis();
                    if(expTk <= currentTime) {
                        LOG.warn("token expired");
                    } else {
                        LOG.info("token is now ok !");
                    }
                } else {
                    LOG.info("token valid");
                }

                LOG.info("Loaded credential from file");
                this.getDriveConnection();
            } else {
                LOG.warn("Loaded credential from file must be expired");
                return Optional.of(this.requiredNewAuth());
            }
        } catch (IOException e) {
            LOG.error("Failed to load credential", e);
        }

//        if(getCredential() != null) {
//            LOG.info("Credentials ok");
//        } else {
//            LOG.error("Credentials failure");
//        }
        return Optional.empty();
    }

    public String requiredNewAuth() {
        String url = authFlow
                .newAuthorizationUrl()
                .setRedirectUri(appHost + oauthCallbackPath)
                .build();

        return url;
    }

    public void grantCallback(String code) {
        LOG.info("Auth granted");

        GoogleTokenResponse tokenResponse = null;
        try {
            //request token
            tokenResponse = authFlow
                    .newTokenRequest(code)
                    .setRedirectUri(appHost + oauthCallbackPath)
                    .execute();

        } catch (IOException e) {
            LOG.error("Failed to request auth token", e);
        }

        try {
            setCredential(authFlow.createAndStoreCredential(tokenResponse, authService.getUsernameFromContext()));

        } catch (IOException e) {
            LOG.error("Failed to create credential", e);
        }

        LOG.info("Credential created");

        getDriveConnection();
    }

    private Runnable connectCallback;

    @Override
    public void connectCallback(Runnable callback) {
        connectCallback = callback;
    }

    private void getDriveConnection() {
        //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authService.getUsernameFromContext();

        //if(mapGetDrive() == null) {
        if(mapDrive == null || mapDrive.containsKey(username) == false) {
            LOG.info("Connecting to Google Drive");
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
            Drive googleDrive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredential())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            mapSetDrive(googleDrive);

            connectCallback.run();

            LOG.info("Schedule refresh token user {}", authService.getUsernameFromContext());
            taskScheduler.schedule(new RefreshTokenTask(ctx, username), OffsetDateTime.now().plusSeconds(tokenRefreshInterval).toInstant());
        } else {
            LOG.info("Already connected to Google Drive");
        }
    }

    public void refreshToken()  {
        //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authService.getUsernameFromContext();
        LOG.info("Refresh token for user {}", username);

        try {
            getCredential().refreshToken();
        } catch (IOException e) {
            LOG.error("Refresh token failed", e);
        }

        LOG.info("Schedule refresh token user {}", authService.getUsernameFromContext());
        taskScheduler.schedule(new RefreshTokenTask(ctx, username), OffsetDateTime.now().plusSeconds(tokenRefreshInterval).toInstant());

        this.getDriveConnection();
        LOG.info("Credential refreshed, gdrive connected {}", mapGetDrive() != null  ? "yes" : "no");
    }

    public Drive getDrive() {
        return mapGetDrive();
    }

}
