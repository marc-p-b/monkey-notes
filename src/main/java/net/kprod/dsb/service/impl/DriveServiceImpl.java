package net.kprod.dsb.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
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
import net.kprod.dsb.WatchExpirationRunnableTask;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.ProcessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class DriveServiceImpl implements DriveService {
    public static final long CHANGES_WATCH_EXPIRATION = 3600;// * 24 * 2;
    public static final int RENEW_OFFSET = 500;
    public static final String GOOGLE_DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    public static final String GOOGLE_APP_DOC_MIME_TYPE = "application/vnd.google-apps.document";
    public static final String GOOGLE_APP_SPREADSHEET_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
    public static final String GOOGLE_APP_PREZ_MIME_TYPE = "application/vnd.google-apps.presentation";

    private Logger LOG = LoggerFactory.getLogger(DriveServiceImpl.class);

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ProcessFile processFile;

    @Value("${app.google.auth.receiver.host}")
    String googleLocalServerReceiverHost;

    @Value("${app.google.auth.receiver.port}")
    int googleLocalServerReceiverPort;

    @Value("${app.url.self}")
    String notifyHostUrl;

    @Value("${app.notify.path}")
    String notifyPath;

    @Value("${app.drive.folders.in}")
    String inboundFolderId;

    @Value("${app.drive.folders.out}")
    String outFolderId;

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final Set<String> SCOPES = DriveScopes.all();
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private String lastPageToken = null;
    private String resourceId = null;
    private Channel channel;
    private Channel responseChannel;
    private Drive drive;
    private String currentChannelId;

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
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
                .setHost(googleLocalServerReceiverHost)
                .setPort(googleLocalServerReceiverPort).build();
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

        //this.checkInboundFile("1PZxb0xf5LlZOEiYKAvT_8zI12K4tuVlj");

    }

    public void watchStop() throws IOException {
        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        drive.channels().stop(responseChannel);
    }

    public void watch() throws IOException {
        currentChannelId = UUID.randomUUID().toString();

        OffsetDateTime odt = OffsetDateTime.now().plusSeconds(CHANGES_WATCH_EXPIRATION);

        channel = new Channel()
                .setExpiration(odt.toInstant().toEpochMilli())
                .setType("web_hook")
                .setAddress(notifyHostUrl + notifyPath)
                .setId(currentChannelId);

        lastPageToken = drive.changes().getStartPageToken().execute().getStartPageToken();
        responseChannel = drive.changes().watch(lastPageToken, channel).execute();

        resourceId = responseChannel.getResourceId();

        long now = System.currentTimeMillis();
        long exp = responseChannel.getExpiration();

        LOG.info("watch response: rs id {} channel id {} lastPageToken {} last for {}", responseChannel.getResourceId(), currentChannelId, lastPageToken, (exp - now));


        ScheduledFuture<?> future = taskScheduler.schedule(new WatchExpirationRunnableTask(ctx), ZonedDateTime.now().plusSeconds(CHANGES_WATCH_EXPIRATION - RENEW_OFFSET).toInstant());

    }

    public void renewWatch() throws IOException {
        LOG.info("renew watch");

        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        drive.channels().stop(responseChannel);

        this.watch();
    }

    private Map<String, ChangedFile> mapScheduled = new HashMap<>();
    private long flushDelay = 12;

    public void getChanges(String channelId) {
        //not from current channel watch
        if(channelId.equals(currentChannelId) == false) {
            return;
        }

        ChangeList changes = null;
        try {
            changes = drive.changes().list(lastPageToken).execute();
            lastPageToken = changes.getNewStartPageToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(!changes.isEmpty()) {
            for (Change change : changes.getChanges()) {
                ChangedFile changedFile = new ChangedFile(change);

                String fileId = change.getFileId();

                if(change.getFile() != null) {
                    LOG.info(" > change fileId {} name {}", fileId, change.getFile().getName());

                    if(checkInboundFile(fileId)) {
                        if(mapScheduled.containsKey(fileId)) {
                            LOG.info("already got a change for file {}", fileId);
                            mapScheduled.get(fileId).getFuture().cancel(true);
                        }
                        LOG.info("accept file change {}", fileId);
                        ScheduledFuture<?> future = taskScheduler.schedule(new ServiceRunnableTask(ctx), ZonedDateTime.now().plusSeconds(flushDelay).toInstant());
                        changedFile.setFuture(future);
                        mapScheduled.put(fileId, changedFile);

                    } else {
                        LOG.info("rejected file {}", fileId);
                    }
                } else {
                    LOG.warn("No file with this id found {}", fileId);
                }
            }
        }
    }

    public void flushChanges() {

        long now = System.currentTimeMillis();
        Set<String> setDone = mapScheduled.entrySet().stream()
                .filter(e->now - e.getValue().getTimestamp() > (flushDelay - 1))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        setDone.forEach(fileId -> {
            Change change = mapScheduled.get(fileId).getChange();
            String filename = change.getFile().getName();
            LOG.info("Flushing fileid {} name {}", fileId, filename);

            Path destPath = Paths.get("/tmp", fileId);
            Path destFile = Paths.get(destPath.toString(), filename);

            if(destPath.toFile().exists()) {
                LOG.info("folder {} already exists", fileId);
                if(destFile.toFile().exists()) {
                    if(destPath.toFile().delete()) {
                        LOG.info("deleted folder {}", fileId);
                    } else {
                        LOG.info("failed to delete folder {}", fileId);
                    }
                }
            } else {
                if(destPath.toFile().mkdir()) {
                    LOG.info("created folder {}", fileId);
                } else {
                    LOG.error("failed to create directory {}", destPath.toFile().getAbsolutePath());
                }
            }

            try {
                LOG.info("download file id {} from gdrive", fileId);
                downloadFile(fileId, destFile);
                LOG.info("Downloaded name {} to {}", filename, destPath);

                LOG.info("async process file id {}", fileId);
                processFile.asyncProcessFile(monitoringService.getCurrentMonitoringData(), fileId, destPath, destFile.toFile());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            mapScheduled.remove(fileId);
        });
    }

    @Override
    public File upload(String name, java.io.File file) {
        LOG.info("upload file {}", name);

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(outFolderId));

        FileContent mediaContent = new FileContent("application/pdf", file);
        try {
            File driveFile = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            System.out.println("File ID: " + driveFile.getId());
            LOG.info("uploaded to drive as fileId {}", driveFile.getId());
            return driveFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void delete(String fileId) {
        LOG.info("delete file {}", fileId);

        try {
            drive.files().delete(fileId).execute();

            LOG.info("deleted file {}", fileId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileName(String fileId) throws IOException {
        File file = drive.files().get(fileId).setFields("name").execute();
        return file.getName();
    }

    public boolean checkInboundFile(String fileId) {
        File file = null;
        try {
            file = drive.files().get(fileId).setFields("parents, mimeType, md5Checksum").execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(file.getMimeType().equals(GOOGLE_DRIVE_FOLDER_MIME_TYPE)) {
            LOG.info("{} is a folder, rejected", fileId);
            return false;
        }

        //level 1
        Optional<String> level1FolderId = checkDriveParents(file.getParents());
        if(level1FolderId.isPresent()) {
            //cache this value
            //LOG.info("parent {} is ok", level1FolderId.get());
            return true;
        } else {
            Optional<String> folderId = recursCheck(file.getParents());
            return folderId.isPresent();
            //LOG.info("parent {} is ok", folderId.orElse("nope !"));
        }

        //return false;
    }

    public Optional<String> recursCheck(List<String> fileIds) {

        List<String> checkedId = new ArrayList<>();

        for(String fileId : fileIds) {
            List<String> lvlParents = getDriveParents(fileId);
            if(lvlParents == null || lvlParents.isEmpty()) {
                //max reached
                return Optional.empty();
            }
            checkedId.addAll(lvlParents);
            Optional<String> lvlFolderId = checkDriveParents(lvlParents);
            if(lvlFolderId.isPresent()) {
                //cache this
                LOG.info("parent {} is ok", lvlFolderId.get());
                return lvlFolderId;
            }
        }
        LOG.info("go recursCheck with ids {}", checkedId);
        return recursCheck(checkedId);

    }

    public List<String> getDriveParents(String fileId) {
        try {
            File dFile = drive.files().get(fileId).setFields("parents").execute();
            return dFile.getParents();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> checkDriveParents(List<String> parents) {
       return parents.stream()
                .filter(id -> inboundFolderId.equals(id))
                .findFirst();
    }


    public void downloadFile(String fileId, Path destinationPath) throws IOException {
        File file = drive.files().get(fileId).setFields("name, mimeType").execute();

        if (file.getMimeType().equals(GOOGLE_APP_DOC_MIME_TYPE) ||
                file.getMimeType().equals(GOOGLE_APP_SPREADSHEET_MIME_TYPE) ||
                file.getMimeType().equals(GOOGLE_APP_PREZ_MIME_TYPE)) {
            throw new IOException("Google App document cannot be downloaded");
        }

        try (OutputStream outputStream = new FileOutputStream(destinationPath.toFile())) {
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        }
    }

    public List<File> listFileByName(String name, String folderId) throws IOException {
        String query = "'" + folderId + "' in parents and name='" + name + "'and trashed = false";
        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();

        return result.getFiles();
    }

    @Override
    public void deleteSimilarNameFromTranscripts(String name) {
        LOG.info("delete previous files {}", name);
        try {
            List<File> list = listFileByName(name, outFolderId);
            for(File file : list) {
                LOG.info("deleted file {} id {}", file.getName(), file.getId());
                delete(file.getId());
            }
        } catch (IOException e) {
            LOG.error("file does not exists with name {}", name, e);
        }
    }
}
