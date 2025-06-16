package net.kprod.dsb.service.impl;

import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.*;
import net.kprod.dsb.data.dto.AsyncProcess;
import net.kprod.dsb.data.entity.*;
import net.kprod.dsb.data.enums.AsyncProcessName;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.repository.*;
import net.kprod.dsb.monitoring.*;
import net.kprod.dsb.service.*;
import net.kprod.dsb.tasks.FlushTask;
import net.kprod.dsb.tasks.RefreshWatchTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
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

    @Value("${app.changes.expiration}")
    private long changesWatchExpiration;

    @Value("${app.changes.flush}")
    private long flushInterval;

    @Value("${app.changes.listen.on-startup.enabled}")
    private boolean changesListenEnabled;

    private ScheduledFuture<?> futureFlush;
    private Map<String, AsyncProcess> mapAsyncProcess2 = new HashMap<>();
    private Map<String, ChangedFile> mapScheduled = new HashMap<>();
    private Map<String, WatchData> mapUsernameWatchData;
    private Map<String, WatchData> mapChannelIdWatchData;

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
    private RepositoryGDriveCredential repositoryGDriveCredential;

    @Autowired
    private AuthService authService;

    @Autowired
    private PreferencesService preferencesService;
    @Autowired
    private RepositoryUser repositoryUser;

    private IdFile idFile(String fileId) {
        return IdFile.createIdFile(authService.getUsernameFromContext(), fileId);
    }

    @EventListener(ApplicationReadyEvent.class)
    void connect() {
        LOG.info("Starting up");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                driveAuthCallBack();
            }
        };
        driveService.connectCallback(runnable);

        LOG.info("Watch drive updates for all users");
//        repositoryGDriveCredential.findAll()
//                .stream()
//                .map(EntityGDriveCredential::getId)
//                .forEach(username -> {
//                    NoAuthContextHolder.setContext(new NoAuthContext(username));
//                    watch(true);
//                });
        NoAuthContextHolder.setContext(new NoAuthContext("marc"));
        watch(true);
    }

    void driveAuthCallBack() {
        LOG.info("Drive auth callback");
        if(changesListenEnabled) {
            boolean watchChanges = false;
            if(mapUsernameWatchData != null && mapUsernameWatchData.containsKey(authService.getUsernameFromContext())) {
                watchChanges = mapUsernameWatchData.get(authService.getUsernameFromContext()).isWatchChanges();
            }
            this.watch(!watchChanges); //if watchChanges is false, this is the first callback ; need to force
        }
    }

    public void updateAll() {
        try {
            updateFolder(preferencesService.getInputFolderId());
        } catch (ServiceException e) {
            LOG.error("Failed to retrieve preferences", e);
        }
    }

    public void watch(boolean renewOrForced) {
        String username = authService.getUsernameFromContext();

        LOG.info("Setup watch drive update for user {}", username);

        String channelId = UUID.randomUUID().toString();


        if(renewOrForced == false) {
            return;
        }
        OffsetDateTime odt = OffsetDateTime.now().plusSeconds(changesWatchExpiration);
        Channel channel = new Channel()
                .setExpiration(odt.toInstant().toEpochMilli())
                .setType("web_hook")
                .setAddress(appHost + notifyPath)
                .setId(channelId);

        WatchData watchData = null;
        try {

            String lastPageToken = driveService.getDrive().changes().getStartPageToken().execute().getStartPageToken();
            Channel responseChannel = driveService.getDrive().changes().watch(lastPageToken, channel).execute();

            watchData = new WatchData()
                .setChannelId(channelId)
                .setUsername(username)
                .setChannel(responseChannel)
                .setLastPageToken(lastPageToken);

        } catch (IOException e) {
            LOG.error("Failed to create watch channel", e);
        }

        watchData.setFutureFlush(taskScheduler.schedule(new RefreshWatchTask(ctx, username), ZonedDateTime.now().plusSeconds(changesWatchExpiration).toInstant()));
        watchData.setWatchChanges(true);

        if(mapChannelIdWatchData == null) {
            mapChannelIdWatchData = new HashMap<>();
        }
        if(mapUsernameWatchData == null) {
            mapUsernameWatchData = new HashMap<>();
        }

        mapUsernameWatchData.put(username, watchData);
        mapChannelIdWatchData.put(channelId, watchData);
        LOG.info("Watch ok for user {}", username);
    }

    @Override
    public void changeNotified(String channelId) {
        if(mapChannelIdWatchData == null || !mapChannelIdWatchData.containsKey(channelId)) {
            LOG.debug("Rejected notified changes channel {}", channelId);
            return;
        }

        WatchData watchData = mapChannelIdWatchData.get(channelId);
        NoAuthContextHolder.setContext(new NoAuthContext(watchData.getUsername()));

        LOG.debug("Changes notified channel {} user {}", channelId, watchData.getUsername());

        String inboundFolderId = "";
        try {
            inboundFolderId = preferencesService.getInputFolderId();
        } catch (ServiceException e) {
            LOG.error("Failed to retrieve inbound folder id {}", inboundFolderId, e);
            return;
        }

        ChangeList changes = null;
        try {
            changes = driveService.getDrive().changes().list(watchData.getLastPageToken()).execute();
            watchData.setLastPageToken(changes.getNewStartPageToken());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(!changes.isEmpty()) {
            for (Change change : changes.getChanges()) {
                ChangedFile changedFile = new ChangedFile(change, watchData.getUsername());

                String fileId = change.getFileId();

                if(change.getFile() != null) {
                    String filename = change.getFile().getName();
                    LOG.debug("Change fileId {} name {}", fileId, filename);

                    //TODO this limits change accetation ONLY to known files
                    if(driveUtilsService.fileHasSpecifiedParents(fileId, inboundFolderId)) {
                        if(mapScheduled.containsKey(fileId)) {
                            LOG.debug("already got a change for file {}", fileId);
                            mapScheduled.get(fileId).getFuture().cancel(true);
                        }
                        LOG.info("Accepted file change id {} name {}", fileId, filename);
                        //todo refresh is deactivated inside task
                        futureFlush = taskScheduler.schedule(new FlushTask(ctx), ZonedDateTime.now().plusSeconds(flushInterval).toInstant());
                        changedFile.setFuture(futureFlush);
                        mapScheduled.put(fileId, changedFile);

                    }
//                    else {
//                        LOG.info("rejected file {}", fileId);
//                    }
                } else {
                    LOG.warn("No file with this id found {}", fileId);
                }
            }
        }
    }

    long CONCURRENT_LIMIT = 1;

    @Override
    public synchronized void flushChanges() {
        LOG.info("Prepare Async (flushChanges)");

        long now = System.currentTimeMillis();
        Map<String, Set<String>> mapAuth2SetFlushedFileId =
                mapScheduled.entrySet().stream()
                //filter changes by time passed since map insertion
                .filter(e -> now - e.getValue().getTimestamp() > (flushInterval - 1))
                //regroup by user
                .collect(Collectors.groupingBy(e->e.getValue().getUsername(), Collectors.mapping(e->e.getKey(), Collectors.toSet())));

        if(concurrentProcessFull()) {
            LOG.warn("Flush skipped, too much concurrent processes");
        }

        try {
            SupplyAsync sa = new SupplyAsync(monitoringService, monitoringService.getCurrentMonitoringData(),
                    () -> asyncProcessFlushed(mapAuth2SetFlushedFileId));
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            // register async process
            long items = mapAuth2SetFlushedFileId.values().stream()
                    .flatMap(s->s.stream())
                    .count();
            String desc = new StringBuilder().append("flushing ").append(items).append(" items").toString();
            registerSyncProcess(AsyncProcessName.flushChanges, monitoringService.getCurrentMonitoringData(), desc, future);

        } catch (ServiceException e) {
            LOG.error("Failed preparing flushChanges async", e);
        }

    }

    private void registerSyncProcess(AsyncProcessName name, MonitoringData monitoringData, String description, CompletableFuture<AsyncResult> future) {

        String id = monitoringData.getId();
        AsyncProcess asyncProcess = new AsyncProcess()
                .setId(id)
                .setFuture(future)
                //todo use enum ?
                .setName(name.name())
                .setCreatedAt(OffsetDateTime.now())
                .setDescription(description);

        mapAsyncProcess2.put(id, asyncProcess);

    }

    private boolean concurrentProcessFull() {
        long count = mapAsyncProcess2.values().stream()
                .filter(p -> p.getName().equals(AsyncProcessName.flushChanges.name()))
                .filter(p -> !p.getFuture().isDone())
                .count();

        if(count >= CONCURRENT_LIMIT) {
            LOG.debug("Concurrent process full ({} processes)", count);
            return true;
        }

        return false;
    }

    public void asyncProcessFlushed(Map<String, Set<String>> mapAuth2SetFlushedFileId) {
        for(Map.Entry<String, Set<String>> e: mapAuth2SetFlushedFileId.entrySet()) {
            //Authentication auth = e.getKey();
            String username = e.getKey();

            Set<String> setFlushedFileId = e.getValue();
            LOG.info("Processing user {} flushed files {}", username, setFlushedFileId.size());

//            SecurityContext context = SecurityContextHolder.createEmptyContext();
//            context.setAuthentication(auth);
//            SecurityContextHolder.setContext(context);

            NoAuthContextHolder.setContext(new NoAuthContext(username));

            asyncProcessFlushedByUser(e.getValue());
        }
    }

    public void asyncProcessFlushedByUser(Set<String> setFlushedFileId) {
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
                        //todo set md5 here ??

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
        LOG.info("Prepare Async (Update folder id {})", folderId);

        Optional<Authentication> optAuth = authService.getLoggedAuthentication();
        if(optAuth.isEmpty()) {
            LOG.error("No authentication found");
            return;
        }

        try {
            SupplyAsyncAuthenticated sa = new SupplyAsyncAuthenticated(monitoringService, monitoringService.getCurrentMonitoringData(),
                    optAuth.get(),
                    () -> asyncUpdateFolder(folderId));
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            registerSyncProcess(AsyncProcessName.updateFolder, monitoringService.getCurrentMonitoringData(), "folder " + folderId, future);
        } catch (ServiceException e) {
            LOG.info("Failed to prepare updateFolder async", e);
        }
    }

    private void asyncUpdateFolder(String folderId) {
        LOG.info("Processing : Update folder id {}", folderId);
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
        LOG.info("Completed : Update folder id {}", folderId);
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
                                //todo : md5 was suppresed by mistake ?
                                .setMd5(file.getMd5Checksum())
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

    @Autowired
    private ImageService imageService;

    public void runListAsyncProcess(List<File2Process> files2Process) {
        // create file objects
        List<EntityFile> listDocs = files2Process.stream()
            .map(f2p -> {
                String fileId = f2p.getFileId();

                Optional<EntityFile> optDoc = repositoryFile.findById(idFile(fileId));
                if (optDoc.isPresent()) {
                    return optDoc.get()
                            //updapte md5
                            .setMd5(f2p.getMd5());
                } else {
                    return f2p.asEntity(authService.getUsernameFromContext());
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

            //prepare pages comparison
            int currentCount = 0;
            Optional<EntityTranscript> t = repositoryTranscript.findById(idFile(file2Process.getFileId()));
            List<BufferedImage> previousImages = new ArrayList<>();
            if(t.isPresent()) {
                currentCount = t.get().getPageCount();
                for(int imageNum = 1; imageNum <= t.get().getPageCount(); imageNum++) {
                    Path imgPath = utilsService.imagePath(authService.getUsernameFromContext(), file2Process.getFileId(), imageNum);
                    try {
                        previousImages.add(ImageIO.read(imgPath.toFile()));
                    } catch (IOException e) {
                        LOG.error("Failed to load image fileid {} page {}", file2Process.getFileId(), imageNum);
                    }
                }
            }

            Path imageWorkingDir = utilsService.downloadDir(file2Process.getFileId());
            List<URL> listImages = pdfService.pdf2Images(
                    authService.getUsernameFromContext(),
                    file2Process.getFileId(),
                    file2Process.getFilePath().toFile(),
                    imageWorkingDir);
            LOG.info("PDF fileId {} file {} image list {}", file2Process.getFileId(), file2Process.getFilePath(), listImages.size());

            //at least one page is modified
            //TODO transcript only modified pages
            boolean isFileModified = false;
            if(listImages.size() == currentCount) {
                //same page count, compare content
                 for(int imageNum = 1; imageNum <= listImages.size(); imageNum++) {
                    try {
                        BufferedImage img = ImageIO.read(utilsService.imagePath(authService.getUsernameFromContext(), file2Process.getFileId(), imageNum).toFile());

//                        // TODO SAVE TO COMP
//                        saveImg(img,"/home/marc/Desktop/_a/new_"+imageNum);
//                        // TODO SAVE TO COMP
//                        saveImg(previousImages.get(imageNum - 1),"/home/marc/Desktop/_a/prev_"+imageNum);


                        double comp = imageService.compareImages(previousImages.get(imageNum - 1), img);

                        if(Double.POSITIVE_INFINITY == comp) {
                            LOG.info("FileId {} page {} UNMODIFIED", file2Process.getFileId(), imageNum);
                        } else {
                            isFileModified = true;
                            LOG.info("FileId {} page {} MODIFIED", file2Process.getFileId(), imageNum);
                        }

                    } catch (IOException e) {
                        LOG.error("Failed to compare images img {} page {}", file2Process.getFileId(), imageNum);
                    }
                }
            } else {
                isFileModified = true;
                LOG.info("FileId {} page count changed", file2Process.getFileId());
            }
            LOG.info("PDF fileId {} status {}", file2Process.getFileId(), (isFileModified ? "modified" : "unmodified"));

            if(isFileModified) {

                List<CompletionResponse> listCompletionResponse = new ArrayList<>();
                for (URL imagePath : listImages) {
                    CompletionResponse completionResponse = qwenService.analyzeImage(
                            file2Process.getFileId(),
                            imagePath);
                    completionResponse.setFile2Process(file2Process);
                    listCompletionResponse.add(completionResponse);
                    if (completionResponse.isCompleted()) {
                        LOG.info("FileId {} Image {} transcript length {}", file2Process.getFileId(), imagePath, completionResponse.getTranscript().length());
                    } else {
                        LOG.info("FileId {} Image {} failed", file2Process.getFileId(), imagePath);
                    }
                }
                LOG.info("Images converted {}", listCompletionResponse.size());

                Map<String, List<CompletionResponse>> mapCompleted = listCompletionResponse.stream()
                        .collect(Collectors.groupingBy(CompletionResponse::getFileId));

                for (Map.Entry<String, List<CompletionResponse>> entry : mapCompleted.entrySet()) {

                    String fileId = entry.getKey();
                    Optional<EntityTranscript> optDoc = repositoryTranscript.findById(idFile(fileId));
                    EntityTranscript entityTranscript = null;
                    if (optDoc.isPresent()) {
                        // already exists transcript
                        entityTranscript = optDoc.get();
                        entityTranscript.bumpVersion();
                    } else {
                        // new transcript
                        entityTranscript = new EntityTranscript()
                                .setIdFile(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));
                    }
                    listCompletionResponse = entry.getValue();

                    int page = 1;
                    for (CompletionResponse completionResponse : listCompletionResponse) {

                        IdTranscriptPage idTranscriptPage = IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), fileId, page++);
                        Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(idTranscriptPage);

                        EntityTranscriptPage entityTranscriptPage = null;
                        if (optPage.isPresent()) {
                            //already exists page
                            entityTranscriptPage = optPage.get();
                            entityTranscriptPage.bumpVersion();
                        } else {
                            //new page
                            entityTranscriptPage = new EntityTranscriptPage()
                                    .setIdTranscriptPage(idTranscriptPage);

                        }
                        if (completionResponse.isCompleted()) {
                            entityTranscriptPage
                                    .setTranscript(completionResponse.getTranscript())
                                    .setAiModel(completionResponse.getAiModel())
                                    .setTranscriptTook(completionResponse.getTranscriptTook())
                                    .setTokensPrompt(completionResponse.getTokensPrompt())
                                    .setTokensResponse(completionResponse.getTokensCompletion())
                                    .setCompleted(true);
                        } else {
                            entityTranscriptPage
                                    .setTranscript("Transcription failed")
                                    .setCompleted(false);
                        }
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

        }
        LOG.info("Done processing files {}", listDocs.size());
    }

//    public static void saveImg(BufferedImage image, String p) {
//
//        try {
//            ImageIO.write(image, "jpg", Paths.get(p).toFile());
//        } catch (IOException e) {
//
//        }
//
//    }

    private OffsetDateTime identifyDates(File2Process f2p) {
        // extract date from title (manually created)
        Pattern titleDatePattern = Pattern.compile("(\\d{6})");
        Matcher m1 = titleDatePattern.matcher(f2p.getFileName());

        OffsetDateTime documentTitleDate = null;
        try {
            if (m1.find()) {
                try {
                    LocalDate ld = LocalDate.parse(m1.group(1), DateTimeFormatter.ofPattern("yyMMdd"));
                    ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                    documentTitleDate = zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime();
                } catch (DateTimeParseException e) {
                    //  todo
                    LOG.warn("Could not parse date in file title {}", m1.group(1), e);
                }
            } else if (f2p.getParentFolderName() != null) {
                Matcher m2 = titleDatePattern.matcher(f2p.getParentFolderName());

                if (m2.find()) {
                    try {
                        LocalDate ld = LocalDate.parse(m2.group(1), DateTimeFormatter.ofPattern("yyMMdd"));
                        ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                        documentTitleDate = zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime();
                    } catch (DateTimeParseException e) {
                        //  todo
                        LOG.warn("Could not parse date in parent title {}", m1.group(1), e);
                    }
                }
            }
        } catch (Exception e) {
            //todo why ??
            LOG.error("Could not identify date in file {}", f2p.getFileId(), e);
        }
        return documentTitleDate;
    }

    @Override
    public void forcePageUpdate(String fileId, int pageNumber) {
        LOG.info("Prepare Async (Force page update for {} page {})", fileId, pageNumber);
        Optional<Authentication> optAuth = authService.getLoggedAuthentication();
        if(optAuth.isEmpty()) {
            LOG.error("No authentication found");
            return;
        }

        try {
            URL imageURL = utilsService.imageURL(optAuth.get().getName(), fileId, pageNumber);
            SupplyAsyncAuthenticated sa = new SupplyAsyncAuthenticated(monitoringService, monitoringService.getCurrentMonitoringData(),
                    optAuth.get(),
                    () -> asyncForcePageUpdate(fileId, pageNumber, imageURL));
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);
            String desc = new StringBuilder().append("forced update file ").append(fileId).append(" page ").append(pageNumber).toString();
            registerSyncProcess(AsyncProcessName.forcePageUpdate, monitoringService.getCurrentMonitoringData(), desc, future);
        } catch (MalformedURLException e) {
            LOG.error("Failed to create image url {}", fileId, e);
        } catch (ServiceException e) {
            LOG.error("Failed to prepare runAsyncForcePageUpdate async", e);
        }
    }



    @MonitoringAsync
    private void asyncForcePageUpdate(String fileId, int pageNumber, URL imageURL) {
        LOG.info("Processing : Force page update for {} page {}", fileId, pageNumber);
        CompletionResponse completionResponse = qwenService.analyzeImage(fileId, imageURL);

        if (completionResponse.isCompleted()) {
            Optional<EntityTranscriptPage> optTranscriptPage = repositoryTranscriptPage.findById(
                    IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), fileId, pageNumber));

            EntityTranscriptPage entityTranscriptPage = null;
            if (optTranscriptPage.isPresent()) {
                //already exists page
                entityTranscriptPage = optTranscriptPage.get();
                entityTranscriptPage.bumpVersion();

            } else {
                LOG.warn("This page may already exists... something wrong ? fileId {}", fileId);
                //new (should not happen)
                entityTranscriptPage = new EntityTranscriptPage()
                        .setIdTranscriptPage(IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), fileId, pageNumber));
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
            //todo throw ?
            LOG.error("Could not force page update for {} page {}", fileId, pageNumber);
        }
        LOG.info("Completed : Force page update for {} page {}", fileId, pageNumber);
    }

    //TODO
    public void watchStop() throws IOException {
//        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
//        driveService.getDrive().channels().stop(responseChannel);
//        watchChanges = false;
    }



    //TODO
    public void renewWatch() throws IOException {
//        LOG.info("renew watch");
//
//        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
//        driveService.getDrive().channels().stop(responseChannel);
//
//        this.watch(true);
    }

    //TODO
    @Override
    public Map<String, Object> getStatus() {
//        List<String> listScheduled = mapScheduled.entrySet().stream()
//                .map(e->{
//                    return new StringBuilder().append(e.getKey()).append(" : ").append(e.getValue()).toString();
//                })
//                .collect(Collectors.toList());
//
//        long delayToFlush = futureFlush != null ? futureFlush.getDelay(TimeUnit.SECONDS) : -1;
//
//        Map<String, Object> info = new HashMap<>();
//        info.put("Watch changes", watchChanges ? "enabled" : "disabled");
//        info.put("Next flush", delayToFlush + "s");
//        info.put("Scheduled count", "" + listScheduled.size());
//        info.put("Scheduled", listScheduled);
//        return info;
        return null;
    }

    @Override
    public void cancelProcess(String id) {
        if(mapAsyncProcess2.get(id) == null) {
            LOG.error("Process does not exists {}", id);
            return;
        }
        LOG.info("Request process cancellation{}", id);
        CompletableFuture future = mapAsyncProcess2.get(id).getFuture();
        future.cancel(true);
        mapAsyncProcess2.remove(id);
        LOG.info("Process cancelled {}", id);
    }

    @Override
    public void requestForceTranscriptUpdate(String fileId) {
        LOG.info("Force update transcript {}", fileId);
        Optional<EntityFile> optEntityFile = repositoryFile.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));
        if(optEntityFile.isPresent()) {
            EntityFile entityFile = optEntityFile.get();
            entityFile.setMd5("");
            repositoryFile.save(entityFile);
            LOG.info("Clean MD5 {}, request update", fileId);
            forceTranscriptUpdate(fileId);
        }
    }

    @Override
    public void forceTranscriptUpdate(String fileId) {
        LOG.info("Prepare Async (Force transcript update for id {} )", fileId);
        Optional<Authentication> optAuth = authService.getLoggedAuthentication();
        if(optAuth.isEmpty()) {
            LOG.error("No authentication found");
            return;
        }

        try {
            SupplyAsyncAuthenticated sa = new SupplyAsyncAuthenticated(monitoringService, monitoringService.getCurrentMonitoringData(),
                    optAuth.get(),
                    () -> asyncForceTranscriptUpdate(fileId));
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            registerSyncProcess(AsyncProcessName.forceTranscriptUpdate, monitoringService.getCurrentMonitoringData(), "update transcript " + fileId, future);
        } catch (ServiceException e) {
            LOG.error("Failed to prepare runAsyncForceTranscriptUpdate", e);
        }
    }

    @MonitoringAsync
    private void asyncForceTranscriptUpdate(String fileId) {
        try {
            File file = driveUtilsService.getDriveFileDetails(fileId);

            File fileParent = driveUtilsService.getDriveFileDetails(file.getParents().get(0));

            Path downloadFileFromDrive = driveUtilsService.downloadFileFromDrive(fileId, file.getName(), utilsService.downloadDir(fileId));

            File2Process file2Process = new File2Process(file)
                    .setFilePath(downloadFileFromDrive)
                    .setParentFolderId(fileParent.getId())
                    .setParentFolderName(fileParent.getName());

            runListAsyncProcess(List.of(file2Process));
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String updateAncestorsFolders(String fileId) throws ServiceException {

        String inboundFolderId = "";
        try {
            inboundFolderId = preferencesService.getInputFolderId();
        } catch (ServiceException e) {
            LOG.error("Failed to retrieve inbound folder id {}", inboundFolderId, e);
            return null;
        }

        File file = driveUtilsService.getDriveFileDetails(fileId);
        List<File> ancestors = driveUtilsService.getAncestorsUntil(file, inboundFolderId, ANCESTORS_RETRIEVE_MAX_DEPTH,null);

        Collections.reverse(ancestors);

        List<EntityFile> folders = new ArrayList<>();
        EntityFile rootFolder = repositoryFile.findByNameAndTypeIs("/", FileType.folder)
                .orElse(new EntityFile()
                        .setIdFile(IdFile.createIdFile(authService.getUsernameFromContext(), inboundFolderId))
                        .setType(FileType.folder)
                        .setName("/"));
        folders.add(rootFolder);

        for(File folderFile : ancestors) {

            EntityFile folder = new EntityFile()
                    .setIdFile(IdFile.createIdFile(authService.getUsernameFromContext(), folderFile.getId()))
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

    @Override
    public Map<String, AsyncProcess> getMapAsyncProcess() {
        return mapAsyncProcess2;
    }

}
