package net.kprod.dsb.service.impl;

import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.*;
import net.kprod.dsb.data.dto.AsyncProcess;
import net.kprod.dsb.data.dto.DtoProcess;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DriveChangeManagerServiceImpl implements DriveChangeManagerService {
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
    private UpdateService updateService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryGDriveCredential repositoryGDriveCredential;

    @Autowired
    private AuthService authService;

    @Autowired
    private PreferencesService preferencesService;



    //TODO UTILS


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
        repositoryGDriveCredential.findAll()
                .stream()
                .map(EntityGDriveCredential::getId)
                .forEach(username -> {
                    NoAuthContextHolder.setContext(new NoAuthContext(username));
                    watch(true);
                });
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

    public void watch(boolean renewOrForced) {
        String username = authService.getUsernameFromContext();

        LOG.info("Setup watch drive update for user {}", username);

        String channelId = UUID.randomUUID().toString();


        if(renewOrForced == false) {
            return;
        }

        if(driveService.getDrive() == null) {
            LOG.warn("Drive is disconnected");
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

                    //TODO this limits change acceptation ONLY to known files
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



        if(processService.concurrentProcessFull()) {
            LOG.warn("Flush skipped, too much concurrent processes");
        }

        try {
            SupplyAsync sa = new SupplyAsync(monitoringService, monitoringService.getCurrentMonitoringData(),
                    () -> asyncProcessFlushed(mapAuth2SetFlushedFileId));
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            // register async process
            //TODO filter already processing fileId ?



            long items = mapAuth2SetFlushedFileId.values().stream()
                    .flatMap(s->s.stream())
                    .count();

            String desc = "";
            if(items <= 10) {
                String itemsList = mapAuth2SetFlushedFileId.values().stream()
                        .flatMap(s -> s.stream())
                        .map(s -> utilsService.getLocalFileName(s))
                        .collect(Collectors.joining(", "));

                desc = new StringBuilder().append("flushing ").append(" items : ").append(itemsList).toString();
            } else {
                desc = new StringBuilder().append("flushing ").append(" items : ").toString();
            }
            processService.registerSyncProcess(AsyncProcessName.flushChanges, monitoringService.getCurrentMonitoringData(), desc, future);
        } catch (ServiceException e) {
            LOG.error("Failed preparing flushChanges async", e);
        }
    }

    public void asyncProcessFlushed(Map<String, Set<String>> mapAuth2SetFlushedFileId) {
        for(Map.Entry<String, Set<String>> e: mapAuth2SetFlushedFileId.entrySet()) {
            String username = e.getKey();

            Set<String> setFlushedFileId = e.getValue();
            LOG.info("Processing user {} flushed files {}", username, setFlushedFileId.size());

            NoAuthContextHolder.setContext(new NoAuthContext(username));
            asyncProcessFlushedByUser(e.getValue());
        }
    }

    private Set<String> setProcessingFileId = new HashSet<>();

    public void asyncProcessFlushedByUser(Set<String> setFlushedFileId) {

        // TODO SYNC !
        //Removed already processing
        Set<String> setFilteredFlushedFileId = setFlushedFileId.stream()
                .filter(fileId -> !setProcessingFileId.contains(fileId))
                .collect(Collectors.toSet());

        setProcessingFileId.addAll(setFilteredFlushedFileId);
        System.out.println("S> "+ setProcessingFileId);
        // TODO SYNC END !

        List<File2Process> files2Process = setFilteredFlushedFileId.stream()
            .map(fileId -> {
                Change change = mapScheduled.get(fileId).getChange();
                String filename = change.getFile().getName();
                LOG.info("Flushing fileid {} name {}", fileId, filename);

                Optional<EntityFile> optDoc = repositoryFile.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));

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

        updateService.runListAsyncProcess(files2Process);
        setProcessingFileId.removeAll(setFilteredFlushedFileId);
        System.out.println("T> "+ setProcessingFileId);
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
}
