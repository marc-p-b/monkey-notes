package net.kprod.dsb.service.impl;

import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import jakarta.annotation.PostConstruct;
import net.kprod.dsb.data.entity.*;
import net.kprod.dsb.tasks.FlushTask;
import net.kprod.dsb.tasks.RefreshWatchTask;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.ChangedFile;
import net.kprod.dsb.data.CompletionResponse;
import net.kprod.dsb.data.DriveFileTypes;
import net.kprod.dsb.data.File2Process;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.data.repository.RepositoryTranscriptPage;
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

import javax.swing.text.html.Option;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DriveChangeManagerServiceImpl implements DriveChangeManagerService {
    //todo props
    public static final int ANCESTORS_RETRIEVE_MAX_DEPTH = 4;
    public static final String MIME_PDF = "application/pdf";

    private Logger LOG = LoggerFactory.getLogger(DriveChangeManagerService.class);

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

    @Value("${app.changes.listen.on-startup.enabled}")
    private boolean changesListenEnabled;

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
    private DriveService driveService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private QwenService qwenService;

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private AuthService authService;

    private IdFile idFile(String fileId) {
        return IdFile.createIdFile(authService.getConnectedUsername(), fileId);
    }

    //todo ?
    @EventListener(ApplicationReadyEvent.class)
    //@PostConstruct
    void startup() {
        LOG.info("Starting up");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                driveAuthCallBack();
            }
        };
        driveService.connectCallback(runnable);
    }

    void driveAuthCallBack() {
        LOG.info("Drive auth callback");
        if(changesListenEnabled) {
            this.watch();
        }
    }

    public void updateAll() {
        updateFolder(inboundFolderId);
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

        //use completable future ?
        processFlushed(monitoringService.getCurrentMonitoringData(), setFlushedFileId);
    }

    public CompletableFuture<AsyncResult> processFlushed(MonitoringData monitoringData, Set<String> setFlushedFileId) {
        SupplyAsync sa = null;

        try {
            sa = new SupplyAsync(monitoringService, monitoringData, () -> asyncProcessFlushed(setFlushedFileId));
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        return CompletableFuture.supplyAsync(sa);
    }

    public void asyncProcessFlushed(Set<String> setFlushedFileId) {

        List<File2Process> files2Process = setFlushedFileId.stream()
                .map(fileId -> {
                    Change change = mapScheduled.get(fileId).getChange();
                    String filename = change.getFile().getName();
                    LOG.info("Flushing fileid {} name {}", fileId, filename);

                    Optional<EntityFile> optDoc = repositoryFile.findById(idFile(fileId));

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

                        Path downloadFileFromDrive = driveUtilsService.downloadFileFromDrive(fileId, file.getName(), utilsService.downloadDir(fileId));

                        File2Process file2Process = new File2Process(file)
                                .setFilePath(downloadFileFromDrive)
                                .setParentFolderId(parentFolder.getId())
                                .setParentFolderName(parentFolder.getName());

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

        runListAsyncProcess(files2Process);
    }

    @Override
    public void updateFolder(String folderId) {
        updateFolder2(monitoringService.getCurrentMonitoringData(), folderId);
    }

    //todo fix naming
    public CompletableFuture<AsyncResult> updateFolder2(MonitoringData monitoringData, String folderId) {
        SupplyAsync sa = null;

        try {
            sa = new SupplyAsync(monitoringService, monitoringData, () -> asyncUpdateFolder(folderId));
        } catch (ServiceException e) {
            LOG.error("Error while processing async folderId {}", folderId, e);
            throw new RuntimeException(e);
        }

        return CompletableFuture.supplyAsync(sa);
    }

    @Async
    public void asyncUpdateFolder(String folderId) {
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
                .filter(f2p -> {
                    return f2p.getMimeType().equals(MIME_PDF);
                })
                .map(file2Process-> {
                        Path targetFolder = utilsService.downloadDir(file2Process.getFileId());
                        Path downloadFilePath = driveUtilsService.downloadFileFromDrive(file2Process.getFileId(), file2Process.getFileName(), targetFolder);
                        file2Process.setFilePath(downloadFilePath);
                        return file2Process;
                })
                .toList();

        runListAsyncProcess(files2Process);
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
                    Optional<EntityFile> optDoc = repositoryFile.findById(idFile(file.getId()));
                    if (file.getTrashed() == true) {
                        LOG.info("File is trashed {} {}", file.getId(), file.getName());
                    } else if (optDoc.isPresent() && optDoc.get().getMd5().equals(file.getMd5Checksum()) == true) {
                        LOG.info("file {} has no changes", file.getId());
                    } else if (
                            (optDoc.isPresent() && optDoc.get().getMd5() == null) ||
                                    (optDoc.isPresent() && optDoc.get().getMd5() != null  && optDoc.get().getMd5().equals(file.getMd5Checksum()) == false) ||
                                    optDoc.isPresent() == false) {
                        //new or updated file
                        remoteFiles.add(new File2Process(file)
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

    public void runListAsyncProcess(List<File2Process> files2Process) {
        // create file objects
        List<EntityFile> listDocs = files2Process.stream()
            .map(f2p -> {
                String fileId = f2p.getFileId();

                Optional<EntityFile> optDoc = repositoryFile.findById(idFile(fileId));
                if (optDoc.isPresent()) {
                    return optDoc.get();
                } else {
                    return f2p.asEntity(authService.getConnectedUsername());
                }
            })
            .toList();
        repositoryFile.saveAll(listDocs);

        for(File2Process file2Process : files2Process) {

            //update full path
            try {
                updateAncestorsFolders(file2Process.getFileId());
            } catch (ServiceException e) {
                LOG.warn("Failed to get full folder path {}", file2Process.getFileId(), e);
            }

            Path imageWorkingDir = utilsService.downloadDir(file2Process.getFileId());
            List<URL> listImages = pdfService.pdf2Images(
                    file2Process.getFileId(),
                    file2Process.getFilePath().toFile(),
                    imageWorkingDir);
            LOG.info("PDF fileId {} file {} image list {}", file2Process.getFileId(), file2Process.getFilePath(), listImages.size());

            List<CompletionResponse> listCompletionResponse = new ArrayList<>();
            for(URL imagePath : listImages) {
                CompletionResponse completionResponse = qwenService.analyzeImage(
                        file2Process.getFileId(),
                        imagePath);
                completionResponse.setFile2Process(file2Process);
                listCompletionResponse.add(completionResponse);
                //todo ok ?
                if(completionResponse.isCompleted()) {
                    LOG.info("FileId {} Image {} transcript length {}", file2Process.getFileId(), imagePath, completionResponse.getTranscript().length());
                } else {
                    LOG.info("FileId {} Image {} failed", file2Process.getFileId(), imagePath) ;
                }
            }
            LOG.info("Images converted {}", listCompletionResponse.size());

            Map<String, List<CompletionResponse>> mapCompleted = listCompletionResponse.stream()
                .filter(CompletionResponse::isCompleted)
                .collect(Collectors.groupingBy(CompletionResponse::getFileId));

            for (Map.Entry<String, List<CompletionResponse>> entry : mapCompleted.entrySet()) {

                String fileId = entry.getKey();
                Optional<EntityTranscript> optDoc = repositoryTranscript.findById(idFile(fileId));
                EntityTranscript entityTranscript = null;
                if(optDoc.isPresent()) {
                    // already exists transcript
                    entityTranscript = optDoc.get();
                    entityTranscript.bumpVersion();
                } else {
                    // new transcript
                    entityTranscript = new EntityTranscript()
                        .setIdFile(IdFile.createIdFile(authService.getConnectedUsername(), fileId));
                }
                listCompletionResponse = entry.getValue();

                int page = 1;
                for (CompletionResponse completionResponse : listCompletionResponse) {

                    IdTranscriptPage idTranscriptPage = IdTranscriptPage.createIdTranscriptPage(authService.getConnectedUsername(), fileId, page++);
                    Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(idTranscriptPage);

                    EntityTranscriptPage entityTranscriptPage = null;
                    if(optPage.isPresent()) {
                        //already exists page
                        entityTranscriptPage = optPage.get();
                        entityTranscriptPage.bumpVersion();
                    } else {
                        //new page
                        entityTranscriptPage = new EntityTranscriptPage()
                                .setIdTranscriptPage(idTranscriptPage);

                    }
                    entityTranscriptPage
                        .setTranscript(completionResponse.getTranscript())
                        .setAiModel(completionResponse.getAiModel())
                        .setTranscriptTook(completionResponse.getTranscriptTook())
                        .setTokensPrompt(completionResponse.getTokensPrompt())
                        .setTokensResponse(completionResponse.getTokensCompletion());
                    repositoryTranscriptPage.save(entityTranscriptPage);
                }

                File2Process f2p = listCompletionResponse.get(0).getFile2Process();

                entityTranscript
                        .setName(f2p.getFileName())
                        .setTranscripted_at(OffsetDateTime.now())
                        .setDocumented_at(identifyDates(f2p))
                        .setPageCount(listCompletionResponse.size());

                repositoryTranscript.save(entityTranscript);

            }
        }
        LOG.info("Done processing files {}", listDocs.size());
    }


    private OffsetDateTime identifyDates(File2Process f2p) {
        // extract date from title (manually created)
        Pattern titleDatePattern = Pattern.compile("(\\d{6})");
        Matcher m1 = titleDatePattern.matcher(f2p.getFileName());

        OffsetDateTime documentTitleDate = null;
        if (m1.find()) {
            try {
                LocalDate ld = LocalDate.parse(m1.group(), DateTimeFormatter.ofPattern("yyMMdd"));
                ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                documentTitleDate = zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime();
            } catch (DateTimeParseException e) {
                //  todo
                LOG.warn("Could not parse date in file title {}", m1.group(1), e);
            }
        } else if (f2p.getParentFolderName() != null) {
            Matcher m2 = titleDatePattern.matcher(f2p.getParentFolderName());

            if(m2.find()) {
                try {
                    LocalDate ld = LocalDate.parse(m2.group(), DateTimeFormatter.ofPattern("yyMMdd"));
                    ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                    documentTitleDate = zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime();
                } catch (DateTimeParseException e) {
                    //  todo
                    LOG.warn("Could not parse date in parent title {}", m1.group(1), e);
                }
            }
        }

        return documentTitleDate;

    }

    public void forcePageUpdate(String fileId, int pageNumber, String model, String prompt) {

        LOG.info("Force page update for {} page {}", fileId, pageNumber);
        try {
            URL imageURL = utilsService.imageURL(fileId, pageNumber);
            runAsyncForcePageUpdate(monitoringService.getCurrentMonitoringData(), fileId, pageNumber, imageURL, model, prompt);
        } catch (MalformedURLException e) {
            //todo error
            LOG.error("Could not parse URL {}", fileId, e);
        }
    }

    @Async
    public void runAsyncForcePageUpdate(MonitoringData monitoringData, String fileId, int pageNumber, URL imageURL, String model, String prompt) {

        SupplyAsync sa = null;

        try {
            sa = new SupplyAsync(monitoringService, monitoringData, () -> asyncForcePageUpdate(fileId, pageNumber, imageURL, model, prompt));
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        CompletableFuture.supplyAsync(sa);
    }

    private void asyncForcePageUpdate(String fileId, int pageNumber, URL imageURL, String model, String prompt) {
        CompletionResponse completionResponse = qwenService.analyzeImage(fileId, imageURL, model, prompt);

        if (completionResponse.isCompleted()) {



            Optional<EntityTranscriptPage> optTranscriptPage = repositoryTranscriptPage.findById(
                    IdTranscriptPage.createIdTranscriptPage(authService.getConnectedUsername(), fileId, pageNumber));

            EntityTranscriptPage entityTranscriptPage = null;
            if (optTranscriptPage.isPresent()) {
                //already exists page
                entityTranscriptPage = optTranscriptPage.get();
                entityTranscriptPage.bumpVersion();

            } else {
                LOG.warn("This page may already exists... something wrong ? fileId {}", fileId);
                //new (should not happen)
                entityTranscriptPage = new EntityTranscriptPage()
                        .setIdTranscriptPage(IdTranscriptPage.createIdTranscriptPage(authService.getConnectedUsername(), fileId, pageNumber));
            }
            entityTranscriptPage
                    .setTranscript(completionResponse.getTranscript())
                    .setTranscriptTook(completionResponse.getTranscriptTook())
                    .setTokensPrompt(completionResponse.getTokensPrompt())
                    .setTokensResponse(completionResponse.getTokensCompletion());

            repositoryTranscriptPage.save(entityTranscriptPage);

            Optional<EntityTranscript> entityTranscript = repositoryTranscript.findById(idFile(fileId));
            if (entityTranscript.isPresent()) {
                entityTranscript.get().bumpVersion();
                repositoryTranscript.save(entityTranscript.get());
            }


        } else {
            LOG.error("Could not force page update for {} page {}", fileId, pageNumber);
        }
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
    public String updateAncestorsFolders(String fileId) throws ServiceException {
        File file = driveUtilsService.getDriveFileDetails(fileId);
        List<File> ancestors = driveUtilsService.getAncestorsUntil(file, inboundFolderId, ANCESTORS_RETRIEVE_MAX_DEPTH,null);

        Collections.reverse(ancestors);

        List<EntityFile> folders = new ArrayList<>();
        EntityFile rootFolder = repositoryFile.findByNameAndTypeIs("/", FileType.folder)
                .orElse(new EntityFile()
                        .setIdFile(IdFile.createIdFile(authService.getConnectedUsername(), inboundFolderId))
                        .setType(FileType.folder)
                        .setName("/"));
        folders.add(rootFolder);

        for(File folderFile : ancestors) {

            EntityFile folder = new EntityFile()
                    .setIdFile(IdFile.createIdFile(authService.getConnectedUsername(), folderFile.getId()))
                    .setType(FileType.folder)
                    .setName(folderFile.getName());

            if(!folderFile.getParents().isEmpty()) {
                folder.setParentFolderId(folderFile.getParents().get(0));
            }
            folders.add(folder);
        }
        repositoryFile.saveAll(folders);
        //todo return dto instead
        return "/" + ancestors.stream().map(File::getName).collect(Collectors.joining("/"));
    }
}
