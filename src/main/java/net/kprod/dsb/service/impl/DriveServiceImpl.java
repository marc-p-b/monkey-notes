package net.kprod.dsb.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import net.kprod.dsb.*;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class DriveServiceImpl implements DriveService {
    private Logger LOG = LoggerFactory.getLogger(DriveServiceImpl.class);

    private static final long TOKEN_REFRESH_INTERVAL = 3500;
    private static final long CHANGES_WATCH_EXPIRATION = 3000;
    private static final long FLUSH_INTERVAL = 60;

    @Value("${app.drive.auth.client-id}")
    private String CLIENT_ID;

    @Value("${app.drive.auth.client-secret}")
    private String CLIENT_SECRET;

    private static final Set<String> SCOPES = Set.of(
            DriveScopes.DRIVE,
            DriveScopes.DRIVE_READONLY,
            DriveScopes.DRIVE_FILE,
            DriveScopes.DRIVE_METADATA,
            DriveScopes.DRIVE_METADATA_READONLY);
    private static final String APPLICATION_NAME = "Drive Notepad Sync";
    private String lastPageToken = null;
    private String resourceId = null;
    private Channel channel;
    private Channel responseChannel;
    private Drive drive;
    private String currentChannelId;
    public static final String GOOGLE_DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    public static final String GOOGLE_APP_DOC_MIME_TYPE = "application/vnd.google-apps.document";
    public static final String GOOGLE_APP_SPREADSHEET_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
    public static final String GOOGLE_APP_PREZ_MIME_TYPE = "application/vnd.google-apps.presentation";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private Credential credential;
    private String refreshToken;

    private GoogleAuthorizationCodeFlow authFlow;
    private NetHttpTransport HTTP_TRANSPORT;
    private Map<String, ChangedFile> mapScheduled = new HashMap<>();

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ProcessFile processFile;

    @Value("${app.url.self}")
    String appHost;

    @Value("${app.notify.path}")
    String notifyPath;

    @Value("${app.oauth-callback.path}")
    String oauthCallbackPath;

    @Value("${app.drive.folders.in}")
    String inboundFolderId;

    @Value("${app.drive.folders.out}")
    String outFolderId;


    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ProcessFileImpl processFileImpl;


    @EventListener(ApplicationReadyEvent.class)
    public void initAuth() {
        HttpTransport httpTransport = new NetHttpTransport();

        //request auth
        //todo storage does not works this way (NPE here : createAndStoreCredential)
        //.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))

        authFlow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPES)

                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        String url = authFlow
                .newAuthorizationUrl()
                .setRedirectUri(appHost + oauthCallbackPath)
                .build();

        LOG.info("Authorise your app through using your browser : {}", url);
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
                    .createAndStoreCredential(tokenResponse, null);
        } catch (IOException e) {
            LOG.error("Failed to create credential", e);
        }

        LOG.info("Credential created");
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        //todo future
        ScheduledFuture<?> future = taskScheduler.schedule(new RefreshTokenTask(ctx), OffsetDateTime.now().plusSeconds(TOKEN_REFRESH_INTERVAL).toInstant());
        this.watch();
    }

    public void refreshToken()  {
        LOG.info("Refresh token");

        credential.setRefreshToken(refreshToken);
        try {
            credential.refreshToken();
        } catch (IOException e) {
            LOG.error("Refresh token failed", e);
        }

        //todo future
        ScheduledFuture<?> future = taskScheduler.schedule(new RefreshTokenTask(ctx), OffsetDateTime.now().plusSeconds(TOKEN_REFRESH_INTERVAL).toInstant());

        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void watchStop() throws IOException {
        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        drive.channels().stop(responseChannel);
    }

    public void watch() {
        currentChannelId = UUID.randomUUID().toString();

        OffsetDateTime odt = OffsetDateTime.now().plusSeconds(CHANGES_WATCH_EXPIRATION);

        channel = new Channel()
                .setExpiration(odt.toInstant().toEpochMilli())
                .setType("web_hook")
                .setAddress(appHost + notifyPath)
                .setId(currentChannelId);

        try {
            lastPageToken = drive.changes().getStartPageToken().execute().getStartPageToken();
            responseChannel = drive.changes().watch(lastPageToken, channel).execute();
        } catch (IOException e) {
            LOG.error("Failed to create watch channel", e);
        }

        resourceId = responseChannel.getResourceId();

        long now = System.currentTimeMillis();
        long exp = responseChannel.getExpiration();

        LOG.info("watch response: rs id {} channel id {} lastPageToken {} last for {}", responseChannel.getResourceId(), currentChannelId, lastPageToken, (exp - now));

        //todo future
        ScheduledFuture<?> future = taskScheduler.schedule(new RefreshWatchTask(ctx), ZonedDateTime.now().plusSeconds(CHANGES_WATCH_EXPIRATION).toInstant());

    }

    public void renewWatch() throws IOException {
        LOG.info("renew watch");

        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        drive.channels().stop(responseChannel);

        this.watch();
    }

    public List<String> getWaitList() {
        return mapScheduled.entrySet().stream()
                .map(e->{
                    return new StringBuilder().append(e.getKey()).append(" : ").append(e.getValue()).toString();
                })
                .collect(Collectors.toList());
    }

    public void getChanges(String channelId) {
        //not from current channel watch
        if(channelId.equals(currentChannelId) == false) {
            LOG.info("Rejected notified changes channel {}", channelId);
            return;
        }
        LOG.info("Changes notified channel {}", channelId);


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
                    LOG.info(" Change fileId {} name {}", fileId, change.getFile().getName());

                    if(checkInboundFile(fileId)) {
                        if(mapScheduled.containsKey(fileId)) {
                            LOG.info("already got a change for file {}", fileId);
                            mapScheduled.get(fileId).getFuture().cancel(true);
                        }
                        LOG.info(" > accept file change {}", fileId);
                        //todo refresh is deactivated inside task
                        ScheduledFuture<?> future = taskScheduler.schedule(new FlushTask(ctx), ZonedDateTime.now().plusSeconds(FLUSH_INTERVAL).toInstant());
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

    //todo have to fix concurrent flush
    public void flushChanges() {
        long now = System.currentTimeMillis();
        Set<String> setDone = mapScheduled.entrySet().stream()
                //filter changes by time passed since map insertion
                .filter(e -> now - e.getValue().getTimestamp() > (FLUSH_INTERVAL - 1))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        //init processing list
        List<File2Process> list2Process = new ArrayList<>();

        setDone.forEach(fileId -> {
            Change change = mapScheduled.get(fileId).getChange();
            String filename = change.getFile().getName();
            LOG.info("Flushing fileid {} name {}", fileId, filename);

            //create a temp folder folder if needed / remove existing
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
            
            //Download file from g drive
            try {
                LOG.info("download file id {} from gdrive", fileId);
                downloadFile(fileId, destFile);
                LOG.info("Downloaded name {} to {}", filename, destPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            //Create file object
            File2Process f2p = new File2Process(fileId, destPath, destFile.toFile());

            //complete file object with md5
            try {
                
                f2p.setMd5(md5(destFile));
            } catch (ServiceException e) {
                throw new RuntimeException(e);
            }
            //Add each file to processing list
            list2Process.add(f2p);
            //remove change
            mapScheduled.remove(fileId);
        });

        //Filter files using md5 / keep only one of each
        List<File2Process> files = list2Process.stream()
            .collect(Collectors.groupingBy(File2Process::getMd5))
            .entrySet().stream().
                map(e -> e.getValue().get(0))
                .toList();

        //Request async file list processing
        processFile.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files);

    }

    private String md5(Path path) throws ServiceException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Files.readAllBytes(path));
            return String.format("%032x", new BigInteger(1, md.digest())); // hex, padded to 32 chars
        } catch (NoSuchAlgorithmException | IOException e) {
            LOG.error("MD5 failed", e);
            throw new ServiceException(e);
        }
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
