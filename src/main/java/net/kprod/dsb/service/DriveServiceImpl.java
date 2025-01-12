package net.kprod.dsb.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.*;
import net.kprod.dsb.ChangedFile;
import net.kprod.dsb.ServiceRunnableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class DriveServiceImpl implements DriveService {
    private Logger LOG = LoggerFactory.getLogger(DriveServiceImpl.class);

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ProcessFile processFile;

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final Set<String> SCOPES = DriveScopes.all();
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private List<ChangedFile> watchedFileChanges;
    private Map<String, UUID> lastChanges;
    private String lastPageToken = null;
    private String resourceId = null;
    private Channel channel;
    private Drive drive;



    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        InputStream in = DriveServiceImpl.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setHost("vps-a2dd2c59.vps.ovh.net")
                .setPort(8087).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        return credential;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void oauthInit() throws IOException, GeneralSecurityException {
        LOG.info("init oauth credentials");
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        LOG.info("connected !");

        this.watch();
    }

    public void watchStop() throws IOException {
        drive.channels().stop(channel);
    }

    public void watch() throws IOException {
        lastChanges = new HashMap<>();
        watchedFileChanges = new ArrayList<>();

        String channelId = UUID.randomUUID().toString();
        String notifyHost = "https://a236-2001-41d0-305-2100-00-1b7f.ngrok-free.app";

        channel = new Channel()
                .setType("web_hook")
                .setAddress(notifyHost + "/notify")
                .setId(channelId);

        lastPageToken = drive.changes().getStartPageToken().execute().getStartPageToken();
        Channel response = drive.changes().watch(lastPageToken, channel).execute();

        resourceId = response.getResourceId();

        long now = System.currentTimeMillis();
        long exp = response.getExpiration();

        LOG.info("watch response: rs id {} channel id {} lastPageToken {} last for {}", response.getResourceId(), channelId, lastPageToken, (exp - now));
    }

    public void getChanges() {
        try {
            ChangeList changes = drive.changes().list(lastPageToken).execute();
            LOG.info("Changes {}", changes.size());
            lastPageToken = changes.getNewStartPageToken();

            if(!changes.isEmpty()) {
                for (Change change : changes.getChanges()) {
                    ChangedFile changedFile = new ChangedFile(change);
                    LOG.info("Added a change fileId {} name {} uuid {} time {}", change.getFileId(), change.getFile().getName(), changedFile.getUuid(), changedFile.getTimestamp().toEpochSecond());

                    watchedFileChanges.add(changedFile);
                    lastChanges.put(change.getFileId(), changedFile.getUuid());
                    taskScheduler.schedule(new ServiceRunnableTask(ctx), ZonedDateTime.now().plusSeconds(10).toInstant());
                }
            }
            else {
                LOG.info("no changes found");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void flushChanges() {

        for(Map.Entry<String, UUID> lastEntry : lastChanges.entrySet()) {

            String fileId = lastEntry.getKey();
            UUID uuid = lastEntry.getValue();

            LOG.info("flush change fileId {} uuid {}", fileId, uuid);

            long remaining = watchedFileChanges.stream()
                            .filter(changedFile -> changedFile.getChange().getFileId().equals(fileId))
                            .count();

            if(remaining > 1) {
                Optional<ChangedFile> first = watchedFileChanges.stream()
                        .sorted(Comparator.comparing(ChangedFile::getTimestamp))
                        .findFirst();
                LOG.info("removed {}", first.get().getTimestamp().toEpochSecond());
                watchedFileChanges.remove(first.get());
            } else {
                Optional<ChangedFile> optLast = watchedFileChanges.stream()
                        .filter(changedFile -> changedFile.getChange().getFileId().equals(fileId))
                        .findFirst();

                Change lastChange = optLast.get().getChange();

                LOG.info(" >> Processing change for fileId{} name {}", lastChange.getFileId(), lastChange.getFile().getName());
                String path = "/tmp/" + lastChange.getFile().getName();
                try {
                    this.downloadFile(lastChange.getFileId(), path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                LOG.info("  >> downloaded to {} ", path);

                processFile.asyncProcessFile(Path.of(path).toFile());
            }
        }
    }

    public String getFileName(String fileId) throws IOException {
        // Appeler la méthode files.get pour récupérer le nom du fichier
        File file = drive.files().get(fileId).setFields("name").execute();
        return file.getName();
    }

    public void downloadFile(String fileId, String destinationPath) throws IOException {
        // Récupérer les informations du fichier
        File file = drive.files().get(fileId).setFields("name, mimeType").execute();

        // Vérifier si le fichier est téléchargeable
        if (file.getMimeType().equals("application/vnd.google-apps.document") ||
                file.getMimeType().equals("application/vnd.google-apps.spreadsheet") ||
                file.getMimeType().equals("application/vnd.google-apps.presentation")) {
            throw new IOException("Le fichier est un document Google. Utilisez l'exportation au lieu du téléchargement.");
        }

        // Télécharger le contenu du fichier
        try (OutputStream outputStream = new FileOutputStream(destinationPath)) {
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        }
    }

    public void list() throws IOException {
        String query = "'1U6QBcbfqhHBmY9lLpk73oCBRMsfNKYvi' in parents and trashed = false";
        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();

        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files in folder:");
            for (File file : files) {
                System.out.printf("File ID: %s, Name: %s\n", file.getId(), file.getName());
            }
        }

    }
}
