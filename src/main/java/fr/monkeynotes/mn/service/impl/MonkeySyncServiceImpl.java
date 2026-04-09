package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.*;
import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.enums.AsyncProcessName;
import fr.monkeynotes.mn.data.enums.FileType;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.data.enums.SyncOption;
import fr.monkeynotes.mn.data.repository.RepositoryFile;
import fr.monkeynotes.mn.monitoring.AsyncResult;
import fr.monkeynotes.mn.monitoring.MonitoringService;
import fr.monkeynotes.mn.monitoring.SupplyAsync;
import fr.monkeynotes.mn.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonkeySyncServiceImpl implements MonkeySyncService {
    private Logger LOG = LoggerFactory.getLogger(UtilsService.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private PreferencesService preferencesService;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private UpdateService updateService;

    @Autowired
    private RepositoryFile repositoryFile;

    private ConcurrentHashMap<String, Set<File2Process>> mapScheduled = new ConcurrentHashMap<>();

    public static final String MONKEYSYNC_ID_PREFIX = "ms";
    public static final String MIME_PDF = "application/pdf";
    public static final String ROOT_FOLDER = "/";

    @Override
    public SyncEventResponse monkeySyncUpdate(MonkeyFileEvent monkeyFileEvent) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(monkeyFileEvent.getContent());

        return monkeySyncUpdate2(monkeyFileEvent, bytes);
    }

    @Override
    public SyncEventResponse monkeySyncUpdate2(MonkeyFileEvent monkeyFileEvent, byte[] fileContent) {

        //Base64.Decoder decoder = Base64.getDecoder();
        //byte[] bytes = decoder.decode(monkeyFileEvent.getContent());

        byte[] hash = null;
        String md5;
        try {
            hash = MessageDigest.getInstance("MD5").digest(fileContent);
            md5 = HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String currentRemoteFolderPath = null;
        String remoteFolderPath = monkeyFileEvent.getRootFolderPath();
        try {
            currentRemoteFolderPath = preferencesService.getPreference(PreferenceKey.remoteRootFolderPath);
        } catch (ServiceException e) {
            //todo is this the best way ?
            preferencesService.setRemoteRootFolderPath(remoteFolderPath);
            currentRemoteFolderPath = remoteFolderPath;
        }

        if(currentRemoteFolderPath == null) {
            preferencesService.setRemoteRootFolderPath(remoteFolderPath);
        }

        if(currentRemoteFolderPath != null && currentRemoteFolderPath.equals(remoteFolderPath) == false) {
            LOG.error("Sync for file {} : remote root folder has changed and cannot be used : {} current folder is : {}",
                    monkeyFileEvent.getFileName(), remoteFolderPath, currentRemoteFolderPath);
            return SyncEventResponse.refusedSyncEventResponse("remote root folder has changed");
        }

        Path path = Paths.get(monkeyFileEvent.getFilePath().replaceAll(monkeyFileEvent.getRootFolderPath(), ""));

        String basePath = path.getParent().toString();
        String filename = path.getFileName().toString();
        String virtualPath = basePath + "/" + filename;

        String msId = createMonkeySyncId(virtualPath);
        String monkeyFolderId = updateAncestorsMonkeyFolders(basePath);

        Path downloadDir = utilsService.downloadDir(monkeyFolderId );
        Path targetFilePath = Paths.get(downloadDir.toString(), msId);

        try {
            Files.write(targetFilePath, fileContent);
        } catch (IOException e) {
            LOG.error("Failed to write file", e);
        }

        File2Process f2p = new File2Process()
                .setFileId(msId)
                .setFileName(filename)
                .setFilePath(targetFilePath)
                .setParentFolderId(monkeyFolderId)
                .setMd5(md5)
                .setMimeType(MIME_PDF)
                .setSyncOption(SyncOption.monkey);

        LOG.info("Adding file name {} id {} status {} - remote path {}",
                f2p.getFileName(), f2p.getFileName(), monkeyFileEvent.getEventType(), virtualPath);

        String username = authService.getUsernameFromContext();

        if (mapScheduled.containsKey(username)) {
            mapScheduled.get(username).add(f2p);
        } else {
            mapScheduled.put(username, new HashSet<>());
            mapScheduled.get(username).add(f2p);
        }

        return SyncEventResponse.acceptedSyncEventResponse(msId);
    }

    @Override
    public void flushMonkeySync(){
        HashMap<String, List<File2Process>> mapScheduledCopy = new HashMap<>();
        mapScheduled.forEach((key, list) ->
                mapScheduledCopy.put(key, new ArrayList<>(list))
        );
        mapScheduled.clear();

        for (Map.Entry<String, List<File2Process>> entry : mapScheduledCopy.entrySet()) {

            String username = entry.getKey();
            List<File2Process> list = entry.getValue();

            SupplyAsync sa = new SupplyAsync(monitoringService, monitoringService.getCurrentMonitoringData(),
                    () -> runListAsyncProcessForUser(list, username));
            processService.registerSyncProcess(username, AsyncProcessName.flushMonkeySyncs, monitoringService.getCurrentMonitoringData(),
                    "flush monkey syncs (" + list.size() + " files)");
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            processService.registerSyncProcessFuture(monitoringService.getCurrentMonitoringData(), future);
        }
    }

    @Override
    public void runListAsyncProcessForUser(List<File2Process> files2Process, String username) {
        NoAuthContextHolder.setContext(new NoAuthContext(username));
        updateService.runListAsyncProcess(files2Process);
    }

    @Override
    public String createMonkeySyncId(String input) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
        byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return MONKEYSYNC_ID_PREFIX + hexString;
    }

    private String updateAncestorsMonkeyFolders(String path) {
        Optional<EntityFile> optRootFolder = repositoryFile.findByIdFile_UsernameAndNameAndTypeIs(authService.getUsernameFromContext(), ROOT_FOLDER, FileType.folder);
        EntityFile rootFolder = null;
        if(optRootFolder.isPresent() == false) {
            // create monkey sync root if necessary
            String msId = createMonkeySyncId(ROOT_FOLDER);
            rootFolder = new EntityFile()
                    .setIdFile(IdFile.createIdFile(authService.getUsernameFromContext(), msId))
                    .setType(FileType.folder)
                    .setName(ROOT_FOLDER);
            repositoryFile.save(rootFolder);

            //also save it as root in prefs
            preferencesService.setInputFolderId(msId);

        } else {
            rootFolder = optRootFolder.get();
        }

        String[] folders = path.split("/");
        StringBuilder sb = new StringBuilder();
        String parentId = rootFolder.getIdFile().getFileId();
        for(String folder : folders) {
            if(folder.isEmpty()) {
                continue;
            }
            sb.append("/").append(folder);

            String currentFolderPath = sb.toString();

            EntityFile entityFileFolder = findOrCreateFolder(createMonkeySyncId(currentFolderPath), currentFolderPath, parentId);
            parentId = entityFileFolder.getIdFile().getFileId();

        }

        return parentId;

    }
    private EntityFile findOrCreateFolder(String id, String path, String parentId) {

        IdFile idFile = IdFile.createIdFile(authService.getUsernameFromContext(), id);

        //TODO force folder type ?
        Optional<EntityFile> optFolder = repositoryFile.findById(idFile);

        if(optFolder.isPresent()) {
            return optFolder.get();
        } else {

            EntityFile folder = new EntityFile()
                    .setIdFile(idFile)
                    .setParentFolderId(parentId)
                    .setType(FileType.folder)
                    .setName(path);

            return repositoryFile.save(folder);
        }
    }


}
