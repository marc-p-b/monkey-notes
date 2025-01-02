//package net.kprod.dsb;
//
//import com.google.api.client.auth.oauth2.Credential;
//import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
//import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
//import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
//import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.client.util.store.FileDataStoreFactory;
//import com.google.api.services.drive.Drive;
//import com.google.api.services.drive.DriveScopes;
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.security.GeneralSecurityException;
//import java.util.Collections;
//import java.util.List;
//
//@Service
//public class StartupServiceImpl implements StartupService {
//    private Logger LOG = LoggerFactory.getLogger(StartupServiceImpl.class);
//    private Drive drive;
//
//    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
//    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//    private static final String TOKENS_DIRECTORY_PATH = "tokens";
//    private static final List<String> SCOPES =
//            Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
//    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
//
//    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
//            throws IOException {
//        // Load client secrets.
//        InputStream in = StartupServiceImpl.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
//        if (in == null) {
//            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
//        }
//        GoogleClientSecrets clientSecrets =
//                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
//
//        // Build flow and trigger user authorization request.
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
//                .setAccessType("offline")
//                .build();
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost("ncs.kassad.xyz").setPort(8087).build();
//        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//        //returns an authorized Credential object.
//        return credential;
//    }
//
//    @PostConstruct
//    public void oauthInit() throws IOException, GeneralSecurityException {
//        // Build a new authorized API client service.
//        LOG.info("init oauth credentials");
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//        LOG.info("connected !");
//
//    }
//
//    public Drive getDrive() {
//        return drive;
//    }
//}
