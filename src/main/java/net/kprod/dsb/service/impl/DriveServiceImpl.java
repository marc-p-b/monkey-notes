package net.kprod.dsb.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import net.kprod.dsb.RefreshTokenTask;
import net.kprod.dsb.service.DriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

@Service
public class DriveServiceImpl implements DriveService {
    public static final String STORED_CREDENTIAL_NAME = "dsb-gdrive-credential";
    private Logger LOG = LoggerFactory.getLogger(DriveServiceImpl.class);

    private static final long TOKEN_REFRESH_INTERVAL = 3500;
    private static final String APPLICATION_NAME = "Drive Notepad Sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${app.drive.auth.client-id}")
    private String CLIENT_ID;

    @Value("${app.drive.auth.client-secret}")
    private String CLIENT_SECRET;

    @Value("${app.drive.auth.credentials.path}")
    private String credentialsPath;

    @Value("${app.url.self}")
    private String appHost;

    @Value("${app.oauth-callback.path}")
    private String oauthCallbackPath;

    private static final Set<String> SCOPES = Set.of(
            DriveScopes.DRIVE,
            DriveScopes.DRIVE_READONLY,
            DriveScopes.DRIVE_FILE,
            DriveScopes.DRIVE_METADATA,
            DriveScopes.DRIVE_METADATA_READONLY);

    private Credential credential;
    private String refreshToken;

    private GoogleAuthorizationCodeFlow authFlow;
    private NetHttpTransport HTTP_TRANSPORT;

    private Drive googleDrive;


    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    //@EventListener(ApplicationReadyEvent.class)
    @Override
    public Optional<String> requireAuth() {

//        if(credential != null) {
//            return Optional.empty();
//        }

        HttpTransport httpTransport = new NetHttpTransport();

        //request auth

        try {
            authFlow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new File(credentialsPath)))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            credential = authFlow.loadCredential(STORED_CREDENTIAL_NAME);

            if (credential != null) {

                long currentTime = System.currentTimeMillis();
                long expTk = credential != null ? credential.getExpirationTimeMilliseconds() : 0;

                if(expTk <= currentTime) {
                    LOG.warn("token expired");
                    //todo not working
                    this.refreshToken();

                    currentTime = System.currentTimeMillis();
                    expTk = credential.getExpirationTimeMilliseconds();
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

        if(credential != null) {
            LOG.info("Credentials ok");
        } else {
            LOG.error("Credentials failure");
        }
        return Optional.empty();
    }

    public String requiredNewAuth() {
        String url = authFlow
                .newAuthorizationUrl()
                .setRedirectUri(appHost + oauthCallbackPath)
                .build();

        //LOG.info("Authorise your app through using your browser : {}", url);
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

        refreshToken = tokenResponse.getRefreshToken();

        try {
            credential = authFlow
                    .createAndStoreCredential(tokenResponse, STORED_CREDENTIAL_NAME);
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
        if(googleDrive == null) {
            LOG.info("Connecting to Google Drive");
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
            googleDrive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            //todo not nececerally required
            connectCallback.run();

            taskScheduler.schedule(new RefreshTokenTask(ctx), OffsetDateTime.now().plusSeconds(TOKEN_REFRESH_INTERVAL).toInstant());
        } else {
            LOG.info("Already connected to Google Drive");
        }
    }

    public void refreshToken()  {
        LOG.info("Refresh token");

        //credential.setRefreshToken(refreshToken);
        try {
            credential.refreshToken();
        } catch (IOException e) {
            LOG.error("Refresh token failed", e);
        }

        taskScheduler.schedule(new RefreshTokenTask(ctx), OffsetDateTime.now().plusSeconds(TOKEN_REFRESH_INTERVAL).toInstant());

        this.getDriveConnection();
//        googleDrive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build();
        LOG.info("Credential refreshed, gdrive connected {}", googleDrive != null  ? "yes" : "no");
    }

    public Drive getDrive() {
        return googleDrive;
    }

}
