package net.kprod.dsb.service.impl;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.*;
import net.kprod.dsb.*;
import net.kprod.dsb.data.entity.Doc;
import net.kprod.dsb.data.repository.DocRepo;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.DriveUtilsService;
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class DriveChangeManagerServiceImpl implements DriveChangeManagerService {
    private Logger LOG = LoggerFactory.getLogger(DriveChangeManagerServiceImpl.class);

    private static final long CHANGES_WATCH_EXPIRATION = 3000;
    private static final long FLUSH_INTERVAL = 15;

    private String lastPageToken = null;
    private String resourceId = null;
    private Channel channel;
    private Channel responseChannel;
    private String currentChannelId;

    private Map<String, ChangedFile> mapScheduled = new HashMap<>();

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ProcessFile processFile;

    @Value("${app.url.self}")
    String appHost;

    @Value("${app.notify.path}")
    String notifyPath;

    @Value("${app.drive.folders.in}")
    String inboundFolderId;

    @Value("${app.drive.folders.out}")
    String outFolderId;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private DocRepo docRepo;

    @Autowired
    private DriveService driveService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Value("${app.erase-db:false}")
    private boolean eraseDb;

    @EventListener(ApplicationReadyEvent.class)
    void startup() {
        LOG.info("Starting up");
        if(eraseDb) {
            LOG.warn(">>> ERASE DB ON STARTUP");
            docRepo.deleteAll();
        }
    }

    public void updateAll() {
        updateFolder(inboundFolderId);
    }

    public void updateFolder(String folderId) {
        File gFolder = null;
        try {
            gFolder = driveService.getDrive().files().get(folderId).setFields("name").execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //update db
        List<Doc> updatedDocs = new ArrayList<>();
        refreshFolder(folderId, "", 4, "", gFolder.getName(), updatedDocs);

        //download files if needed
        updatedDocs.stream()
                .filter(Doc::isMarkForUpdate)
                .forEach(d-> {
                        Path destPath = Paths.get("/tmp", d.getFileId());
                        Path destFile = Paths.get(destPath.toString(), d.getFileName());
                        driveUtilsService.downloadFileFromDrive(d.getFileId(), destPath, destFile, d.getFileName());
                        d.setLocalFolder(destPath.toString());
                        d.setMarkForUpdate(false);
                });
        docRepo.saveAll(updatedDocs);

        LOG.info("Downloaded files");

        List<File2Process> files2Process = updatedDocs.stream()
            .map(d->{
                Path path2File = Paths.get(d.getLocalFolder(), d.getFileName());
                return new File2Process(d.getFileId(), Paths.get(d.getLocalFolder()), path2File.toFile());
            })
            .toList();
        processFile.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files2Process);
    }

    public void watchStop() throws IOException {
        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        driveService.getDrive().channels().stop(responseChannel);
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
            lastPageToken = driveService.getDrive().changes().getStartPageToken().execute().getStartPageToken();
            responseChannel = driveService.getDrive().changes().watch(lastPageToken, channel).execute();
        } catch (IOException e) {
            LOG.error("Failed to create watch channel", e);
        }

        resourceId = responseChannel.getResourceId();

        long now = System.currentTimeMillis();
        long exp = responseChannel.getExpiration();

        LOG.info("watch response: rs id {} channel id {} lastPageToken {} last for {}", responseChannel.getResourceId(), currentChannelId, lastPageToken, (exp - now));

        taskScheduler.schedule(new RefreshWatchTask(ctx), ZonedDateTime.now().plusSeconds(CHANGES_WATCH_EXPIRATION).toInstant());
    }

    public void renewWatch() throws IOException {
        LOG.info("renew watch");

        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        driveService.getDrive().channels().stop(responseChannel);

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
            changes = driveService.getDrive().changes().list(lastPageToken).execute();
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

                    if(driveUtilsService.fileHasSpecifiedParents(fileId, inboundFolderId)) {
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
    @Override
    public synchronized void flushChanges() {
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


            Optional<Doc> optDoc = docRepo.findById(fileId);

            File file;
            try {
                file = driveService.getDrive().files().get(fileId).setFields("id, name, mimeType, md5Checksum").execute();
            } catch (IOException e) {
                //todo
                throw new RuntimeException(e);
            }

            File2Process file2Process = null;
            if(optDoc.isPresent() && file.getMd5Checksum().equals(optDoc.get().getMd5()) == false) {
                //update
                file2Process = new File2Process()
                        .setFileId(fileId)
                        .setMd5(file.getMd5Checksum());
                LOG.info("update file {} {}", fileId, file.getName());

            } else if (optDoc.isPresent() == false) {
                //create
                file2Process = new File2Process()
                        .setFileId(fileId)
                        .setMd5(file.getMd5Checksum());
                LOG.info("create file {} {}", fileId, file.getName());
            } else {
                LOG.info("do nothing file {} {}", fileId, file.getName());

            }

            if(file2Process != null) {
                //create a temp folder folder if needed / remove existing
                Path destPath = Paths.get("/tmp", fileId);
                Path destFile = Paths.get(destPath.toString(), filename);
                File file2Download = driveUtilsService.downloadFileFromDrive(fileId, destPath, destFile, filename);


                //Create file object
                //File2Process f2p = new File2Process(fileId, Paths.get("/tmp", fileId), destFile.toFile());
                //f2p.setMd5(file2Download.getMd5Checksum());
                file2Process
                    .setWorkingDir(destPath)
                    .setFile(destFile.toFile());

                //Add each file to processing list
                list2Process.add(file2Process);
            }
            //remove change
            mapScheduled.remove(fileId);
        });

        //Filter files using md5 / keep only one of each
        List<File2Process> files = list2Process.stream()
            .collect(Collectors.groupingBy(File2Process::getMd5))
            .entrySet().stream().
                map(e -> e.getValue().get(0))
                .toList();

        List<Doc> docs = files.stream()
                .map(f2p -> {
                    return new Doc(f2p.getFileId(), f2p.getFile().getName(), "UNKNOWN", f2p.getMd5()).setMarkForUpdate(true);
                })
                .toList();

        docRepo.saveAll(docs);


        //Request async file list processing
        processFile.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files);

    }

    @Override
    public File processTranscript(String name, String fileId, String transcript, java.io.File file) {
        LOG.info("upload file id {} name {}", fileId, name);

        Optional<Doc> optDoc = docRepo.findById(fileId);
        if(optDoc.isPresent()) {
            Doc doc = optDoc.get();
            doc.setTranscripted_at(OffsetDateTime.now());
            doc.setTranscript(transcript);
            doc.setMarkForUpdate(false);
            docRepo.save(doc);
        } else {
            LOG.error("doc not found {}", fileId);
        }

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(outFolderId));

        FileContent mediaContent = new FileContent("application/pdf", file);
        try {
            File driveFile = driveService.getDrive().files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            System.out.println("File ID: " + driveFile.getId());
            LOG.info("uploaded to drive as fileId {}", driveFile.getId());
            return driveFile;
        } catch (IOException e) {
            LOG.error("error uploading file {}", fileId, e);
        }

        return null;
    }

    @Override
    public void refreshFolder(String folderId, String offset, int max_depth, String folder, String currentFolderName, List<Doc> updatedDocs) {
        String query = "'" + folderId + "' in parents and trashed = false";

        FileList result = null;
        try {
             result = driveService.getDrive().files().list()
                    .setQ(query)
                    .setFields("files(id, mimeType, md5Checksum, name)")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            //System.out.println("No files found.");
        } else {

            for (File file : files) {
                //LOG.info("filename {} id {}", file.getName(), file.getId());

                if(file.getMimeType() != null && file.getMimeType().equals(DriveFileTypes.GOOGLE_DRIVE_FOLDER_MIME_TYPE) && max_depth > 0) {
                    LOG.info("{}{} ({})/",offset, file.getName(), max_depth);
                    refreshFolder(file.getId(), offset + " ", max_depth - 1, folder + "/" + file.getName(), file.getName(), updatedDocs);

                } else {
                    LOG.info(offset + "{} ({})" ,file.getName(), file.getMd5Checksum());

                    Optional<Doc> optDoc = docRepo.findById(file.getId());
                    Doc doc = null;
                    if(optDoc.isPresent() && optDoc.get().getMd5().equals(file.getMd5Checksum()) == false) {
                        //doc has to be updated
                        doc = optDoc.get();
                        doc.setMarkForUpdate(true);

                    } else if (optDoc.isPresent() == false) {
                        //new file : create doc
                        doc = new Doc(file.getId(), file.getName(), folder, file.getMd5Checksum())
                                .setParentFolderId(folderId)
                                .setParentFolderName(currentFolderName)
                                .setMarkForUpdate(true);
                    }
                    docRepo.save(doc);
                    updatedDocs.add(doc);
                }
            }
        }
    }


}
