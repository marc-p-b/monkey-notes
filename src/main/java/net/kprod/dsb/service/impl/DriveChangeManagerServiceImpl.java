package net.kprod.dsb.service.impl;

import com.google.api.services.drive.model.*;
import net.kprod.dsb.FlushTask;
import net.kprod.dsb.RefreshWatchTask;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.ChangedFile;
import net.kprod.dsb.data.CompletionResponse;
import net.kprod.dsb.data.DriveFileTypes;
import net.kprod.dsb.data.File2Process;
import net.kprod.dsb.data.entity.Doc;
import net.kprod.dsb.data.repository.RepoDoc;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringData;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.monitoring.SupplyAsync;
import net.kprod.dsb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DriveChangeManagerServiceImpl implements DriveChangeManagerService {
    public static final int ANCESTORS_RETRIEVE_MAX_DEPTH = 4;
    private Logger LOG = LoggerFactory.getLogger(DriveChangeManagerServiceImpl.class);

    @Value("${app.url.self}")
    private String appHost;

    @Value("${app.notify.path}")
    private String notifyPath;

    @Value("${app.drive.folders.in}")
    private String inboundFolderId;

    @Value("${app.drive.folders.out}")
    private String outFolderId;

    @Value("${app.erase-db:false}")
    private boolean eraseDb;

    @Value("${app.changes.expiration}")
    private long changesWatchExpiration;

    @Value("${app.changes.flush}")
    private long flushInterval;

    @Value("${app.paths.download}")
    private String downloadPath;

    private String lastPageToken = null;
    private String resourceId = null;
    private Channel channel;
    private Channel responseChannel;
    private String currentChannelId;

    private Map<String, ChangedFile> mapScheduled = new HashMap<>();
    private ScheduledFuture<?> futureFlush;
    private boolean watchChanges = false;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private RepoDoc repoDoc;

    @Autowired
    private DriveService driveService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private QwenService qwenService;

    @EventListener(ApplicationReadyEvent.class)
    void startup() {
        LOG.info("Starting up");
        if(eraseDb) {
            LOG.warn(">>> ERASE DB ON STARTUP");
            repoDoc.deleteAll();
        }
    }

    public void updateAll() {
        updateFolder(inboundFolderId);
    }

    //TODO async !
    @Override
    public void updateFolder(String folderId) {
        File gFolder = null;
        try {
            //check this is a folder
            gFolder = driveUtilsService.getDriveFileDetails(folderId);
        } catch (ServiceException e) {
            LOG.error("failed to get drive folder", e);
            return;
        }
        List<File2Process> remoteFiles = recursRefreshFolder(folderId, "", 4, "", gFolder.getName(), null);

        List<File2Process> files2Process = remoteFiles.stream()
                .map(file2Process-> {
                        Path targetFolder = fileWorkingDir(file2Process.getFileId());
                        Path downloadFilePath = driveUtilsService.downloadFileFromDrive(file2Process.getFileId(), file2Process.getFileName(), targetFolder);
                        file2Process.setFilePath(downloadFilePath);
                        return file2Process;
                })
                .toList();

        //Legacy processing using shell and python
        //legacyProcessFile.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files2Process);
        this.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files2Process);
    }

    private List<File2Process> recursRefreshFolder(String currentFolderId, String offset, int max_depth, String currentFolderPath, String currentFolderName, List<File2Process> remoteFiles) {
        if (remoteFiles == null) {
            remoteFiles = new ArrayList<>();
        }
        List<File> files = null;
        try {
            files = driveUtilsService.listDriveFilesPropertiesFromFolder(currentFolderId).getFiles();
        } catch (ServiceException e) {
            LOG.error("failed to list files in folder {}", currentFolderId);
            return Collections.emptyList();
        }

        if (files == null || files.isEmpty()) {
            LOG.error("empty folder {}", currentFolderId);
            return Collections.emptyList();
        } else {
            for(File file : files) {
                if(file.getMimeType() != null && file.getMimeType().equals(DriveFileTypes.GOOGLE_DRIVE_FOLDER_MIME_TYPE) && max_depth > 0) {
                    LOG.info("{}{} ({})/",offset, file.getName(), max_depth);
                    recursRefreshFolder(file.getId(), offset + " ", max_depth - 1, currentFolderPath + "/" + file.getName(), file.getName(), remoteFiles);

                } else {
                    LOG.info(offset + "{} ({})" ,file.getName(), file.getMd5Checksum());
                    Optional<Doc> optDoc = repoDoc.findById(file.getId());
                    if (file.getTrashed() == true) {
                        LOG.info("File is trashed {} {}", file.getId(), file.getName());
                    } else if (optDoc.isPresent() && optDoc.get().getMd5().equals(file.getMd5Checksum()) == true) {
                        LOG.info("file {} has no changes", file.getId());
                    } else if (
                            (optDoc.isPresent() && optDoc.get().getMd5() == null) ||
                                    (optDoc.isPresent() && optDoc.get().getMd5() != null  && optDoc.get().getMd5().equals(file.getMd5Checksum()) == false) ||
                                    optDoc.isPresent() == false) {
                        //new or updated file
                        remoteFiles.add(new File2Process(file.getId())
                                .setFileName(file.getName())
                                .setMd5(file.getMd5Checksum())
                                //.setDriveFullFolderPath(currentFolderPath) //TODO maybe relative / fix this ?
                                .setParentFolderId(currentFolderId)
                                .setParentFolderName(currentFolderName));
                    } else {
                        LOG.warn("Nothing to do with file {} {}", file.getId(), file.getName());
                    }
                }
            }
        }
        return remoteFiles;
    }

    @Async
    @Override
    public CompletableFuture<AsyncResult> asyncProcessFiles(MonitoringData monitoringData, List<File2Process> list) {
        SupplyAsync sa = null;

        try {
            sa = new SupplyAsync(monitoringService, monitoringData, () -> runListAsyncProcess(list));
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        return CompletableFuture.supplyAsync(sa);
    }

    public void runListAsyncProcess(List<File2Process> files2Process) {
        //Map fileId -> CompletionResponse
        Map<String, List<CompletionResponse>> mapCompleted = files2Process.stream()
                .flatMap(file2Process-> {
                    Path workingDir = fileWorkingDir(file2Process.getFileId());
                    List<Path> listImages = pdfService.pdf2Images(
                            file2Process.getFileId(),
                            file2Process.getFilePath().toFile(),
                            workingDir);
                    LOG.info("PDF fileId {} file {} image list {}", file2Process.getFileId(), file2Process.getFilePath(), listImages.size());
                    return listImages.stream()
                            .map(imagePath->{
                                CompletionResponse completionResponse = qwenService.analyzeImage(
                                        imagePath,
                                        file2Process.getFileId(),
                                        imagePath.getFileName().toString());
                                completionResponse.setFile2Process(file2Process);
                                LOG.info("FileId {} Image {} transcript length {}", file2Process.getFileId(), imagePath.getFileName(), completionResponse.getTranscript().length());
                                return completionResponse;
                            });
                })
                .collect(Collectors.groupingBy(CompletionResponse::getFileId));

        List<Doc> list = mapCompleted.entrySet().stream()
                .map(entry -> {
                    String fileId = entry.getKey();

                    Optional<Doc> optDoc = repoDoc.findById(fileId);
                    Doc doc = null;
                    if(optDoc.isPresent()) {
                        doc = optDoc.get();
                        doc.setVersion(doc.getVersion() + 1);
                    } else {
                        doc = new Doc();
                    }
                    List<CompletionResponse> listCompletionResponse = entry.getValue();

                    long took = listCompletionResponse.stream()
                            .map(CompletionResponse::getTranscriptTook).reduce(0l, Long::sum);
                    int tokensPrompt = listCompletionResponse.stream()
                            .map(CompletionResponse::getTokensPrompt).reduce(0, Integer::sum);
                    int tokensCompletion = listCompletionResponse.stream()
                            .map(CompletionResponse::getTokensCompletion).reduce(0, Integer::sum);

                    File2Process f2p = listCompletionResponse.get(0).getFile2Process();

                    List<String> transcripts = listCompletionResponse.stream()
                            .map(CompletionResponse::getTranscript)
                            .toList();

                    //move page numbering to image analysis ?
                    int page = 1;
                    StringBuilder sbTranscripts = new StringBuilder();
                    for(String transcript : transcripts) {
                        sbTranscripts
                            .append(page == 1 ? "" : "\n\n")
                            .append("## Page ").append(page++).append("\n\n").append(transcript);
                    }

                    //retrieve full path
                    String fullFolderPath = null;
                    try {
                        fullFolderPath = getAncestors(fileId);
                    } catch (ServiceException e) {
                        throw new RuntimeException(e);
                    }

                    doc
                        .setFileId(fileId)
                        .setTranscripted_at(OffsetDateTime.now())
                        .setFileName(f2p.getFileName())
                        .setRemoteFolder(fullFolderPath)
                        .setParentFolderId(f2p.getParentFolderId())
                        .setParentFolderName(f2p.getParentFolderName())
                        .setMd5(f2p.getMd5())
                        .setAiModel(listCompletionResponse.get(0).getAiModel())
                        .setPageCount(listCompletionResponse.size())
                        .setTokensPrompt(tokensPrompt)
                        .setTokensResponse(tokensCompletion)
                        .setTranscriptTook(took)
                        .setTranscript(sbTranscripts.toString());
                    return doc;
                })
                .toList();

        //error here / Entity must not be null
        repoDoc.saveAll(list);
    }

    @Override
    public void getChanges(String channelId) {
        //not from current channel watch
        if(channelId.equals(currentChannelId) == false) {
            LOG.debug("Rejected notified changes channel {}", channelId);
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
                        futureFlush = taskScheduler.schedule(new FlushTask(ctx), ZonedDateTime.now().plusSeconds(flushInterval).toInstant());
                        changedFile.setFuture(futureFlush);
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

    @Override
    public synchronized void flushChanges() {
        long now = System.currentTimeMillis();
        Set<String> setFlushedFileId = mapScheduled.entrySet().stream()
                //filter changes by time passed since map insertion
                .filter(e -> now - e.getValue().getTimestamp() > (flushInterval - 1))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<File2Process> files2Process = setFlushedFileId.stream()
                .map(fileId -> {
                    Change change = mapScheduled.get(fileId).getChange();
                    String filename = change.getFile().getName();
                    LOG.info("Flushing fileid {} name {}", fileId, filename);


                    Optional<Doc> optDoc = repoDoc.findById(fileId);


                    File file = null;
                    try {
                        file = driveUtilsService.getDriveFileDetails(fileId);
                    } catch (ServiceException e) {
                        LOG.error("Error getting file details for {}", fileId, e);
                    }

                    //TODO here we assume that only 1 parent per file is possible...
                    File parentFolder = null;
                    if (file.getParents().size() > 0) {
                        try {
                            parentFolder = driveUtilsService.getDriveFileDetails(file.getParents().get(0));
                        } catch (ServiceException e) {
                            LOG.error("Error getting file details for {}", fileId, e);
                        }
                    }


                    Optional<File2Process> returnObject = Optional.empty();
                    //File2Process file2Process = null;
                    if (file.getTrashed() == true) {
                        LOG.info("File is trashed {} {}", filename, fileId);
                    } else if (optDoc.isPresent() && optDoc.get().getMd5().equals(file.getMd5Checksum()) == true) {
                        LOG.info("File {} {} has no changes", file.getId(), filename);
                    } else if (
                        (optDoc.isPresent() && optDoc.get().getMd5() == null) ||
                        (optDoc.isPresent() && optDoc.get().getMd5() != null && file.getMd5Checksum().equals(optDoc.get().getMd5()) == false) ||
                        optDoc.isPresent() == false) {
                        LOG.info("create or update file {} {}", fileId, file.getName());

                        Path downloadFileFromDrive = driveUtilsService.downloadFileFromDrive(fileId, file.getName(), fileWorkingDir(fileId));

                        File2Process file2Process = new File2Process(fileId)
                                .setFileName(file.getName())
                                .setFilePath(downloadFileFromDrive)
                                .setParentFolderId(parentFolder.getId())
                                .setParentFolderName(parentFolder.getName())
                                .setMd5(file.getMd5Checksum());

                        //list2Process.add(file2Process);
                        returnObject = Optional.of(file2Process);
                    }  else {
                        LOG.warn("Nothing to do with file {} {}", file.getId(), file.getName());
                    }
                    //remove change
                    mapScheduled.remove(fileId);
                    return returnObject;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        //Filter files using md5 / keep only one of each
        files2Process = files2Process.stream()
            .collect(Collectors.groupingBy(File2Process::getMd5))
            .entrySet().stream().
                map(e -> e.getValue().get(0))
                .toList();


        this.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files2Process);

        //Request async file list processing
        //legacyProcessFile.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files);
    }



    public void watchStop() throws IOException {
        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        driveService.getDrive().channels().stop(responseChannel);
        watchChanges = false;
    }

    public void watch() {
        currentChannelId = UUID.randomUUID().toString();

        OffsetDateTime odt = OffsetDateTime.now().plusSeconds(changesWatchExpiration);

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

        taskScheduler.schedule(new RefreshWatchTask(ctx), ZonedDateTime.now().plusSeconds(changesWatchExpiration).toInstant());
        watchChanges = true;
    }

    public void renewWatch() throws IOException {
        LOG.info("renew watch");

        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        driveService.getDrive().channels().stop(responseChannel);

        this.watch();
    }

    @Override
    public Map<String, Object> getStatus() {
        List<String> listScheduled = mapScheduled.entrySet().stream()
                .map(e->{
                    return new StringBuilder().append(e.getKey()).append(" : ").append(e.getValue()).toString();
                })
                .collect(Collectors.toList());

        long delayToFlush = futureFlush != null ? futureFlush.getDelay(TimeUnit.SECONDS) : -1;

        Map<String, Object> info = new HashMap<>();
        info.put("Watch changes", watchChanges ? "enabled" : "disabled");
        info.put("Next flush", delayToFlush + "s");
        info.put("Scheduled count", "" + listScheduled.size());
        info.put("Scheduled", listScheduled);
        return info;
    }

    @Override
    public List<String> listAvailableTranscripts() {
        return repoDoc.findAll().stream() //optimize request
                .filter(d -> d.getTranscripted_at() != null)
                .map(d -> {
                    return new StringBuilder()
                            .append(d.getFileId()).append(" - ").append(d.getFileName())
                            .toString();
                })
                .toList();
    }

    @Override
    public String getTranscript(String fileId) {
        Optional<Doc> optDoc = repoDoc.findById(fileId);
        if (optDoc.isPresent()) {
            Doc doc = optDoc.get();
            return doc.getTranscript();
        }
        return "no transcript found for " + fileId;
    }


    @Override
    public String getAncestors(String fileId) throws ServiceException {
        File file = driveUtilsService.getDriveFileDetails(fileId);
        List<File> ancestors = driveUtilsService.getAncestorsUntil(file, inboundFolderId, ANCESTORS_RETRIEVE_MAX_DEPTH,null);

        Collections.reverse(ancestors);

        return "/" + ancestors.stream().map(File::getName).collect(Collectors.joining("/"));
    }


    private void removeFileIfExists(Path target) {
        if(target.toFile().exists()) {
            if(target.toFile().delete()) {
                LOG.info("deleted file {}", target);
            } else {
                LOG.info("failed to delete folder {}", target);
            }
        }
    }

    private Path fileWorkingDir(String fileId) {

        Path pathDownloadFolder = Paths.get(downloadPath, fileId);

        if(pathDownloadFolder.toFile().exists()) {
            LOG.info("folder {} already exists", fileId);

        } else {
            if(pathDownloadFolder.toFile().mkdir()) {
                LOG.info("created folder {}", fileId);
            } else {
                LOG.error("failed to create directory {}", pathDownloadFolder);
            }
        }

        return pathDownloadFolder;
    }


}
