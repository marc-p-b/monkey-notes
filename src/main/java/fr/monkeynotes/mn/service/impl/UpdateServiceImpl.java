package fr.monkeynotes.mn.service.impl;

import com.google.api.services.drive.model.File;
import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.*;
import fr.monkeynotes.mn.data.dto.AsyncProcessFileEvent;
import fr.monkeynotes.mn.data.entity.*;
import fr.monkeynotes.mn.data.enums.AsyncProcessName;
import fr.monkeynotes.mn.data.enums.FileType;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.data.repository.RepositoryFile;
import fr.monkeynotes.mn.data.repository.RepositoryMonkeyFile;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPage;
import fr.monkeynotes.mn.monitoring.*;
import fr.monkeynotes.mn.service.*;
import fr.monkeynotes.mn.tasks.FlushMonkeySyncTask;
import fr.monkeynotes.mn.utils.TranscriptUtils;
import fr.monkeynotes.mn.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UpdateServiceImpl implements UpdateService {
    public static final String ROOT_FOLDER = "/";
    private Logger LOG = LoggerFactory.getLogger(UpdateServiceImpl.class);

    //todo props or conf
    public static final int ANCESTORS_RETRIEVE_MAX_DEPTH = 4;
    public static final String MIME_PDF = "application/pdf";

    @Autowired
    private AuthService authService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private QwenService qwenService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private PreferencesService preferencesService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private NamedEntitiesService namedEntitiesService;

    @Autowired
    private RepositoryMonkeyFile repositoryMonkeyFile;

    @Override
    public void runListAsyncProcess(List<File2Process> files2Process) {

        final String processId = monitoringService.getCurrentMonitoringData().getId();
        processService.updateProcess(processId, "files to process : " + files2Process.size());

        for(File2Process file2Process : files2Process) {

            // --------------------------------------
            // Legacy file : get full path of the current file
            // --------------------------------------
            if (file2Process.isLegacy()) {
                try {
                    updateAncestorsFoldersGDrive(file2Process);
                } catch (ServiceException e) {
                    LOG.warn("Failed to get full folder path {}", file2Process.getFileId(), e);
                }
            }

            AsyncProcessFileEvent fileEvent = new AsyncProcessFileEvent(file2Process.getFileId(), file2Process.getFileName(), file2Process.getParentFolderName());
            processService.attachFileEvent(processId, fileEvent);

            // --------------------------------------
            // PDF 2 images / temp dir
            // --------------------------------------
            List<URL> listImages = pdfService.pdf2Images(
                    authService.getUsernameFromContext(),
                    file2Process.getFileId(),
                    file2Process.getFilePath().toFile());
            LOG.info("PDF fileId {} file {} image list {}", file2Process.getFileId(), file2Process.getFilePath(), listImages.size());
            fileEvent.setTotalPages(listImages.size());


            // --------------------------------------
            // Get list of modified or created images
            // --------------------------------------
            Images2Process images2Process = getModifiedOrNewImages2Process(file2Process, listImages);
            List<Image2Process> modifiedOrNewImages = images2Process.getListImage2Process();
            LOG.info("PDF fileId {} has {} modified or new pages", file2Process.getFileId(), modifiedOrNewImages.size());

            processService.updateProcess(processId, "created or modified pages : " + modifiedOrNewImages.size());
            fileEvent.setModifiedPages(modifiedOrNewImages.size());

            if(modifiedOrNewImages.isEmpty()) {
                LOG.info("No new or modified images, exiting from update process");
                return;
            }

            // --------------------------------------
            // Move new files to regular dir
            // --------------------------------------
            Path tempDir = utilsService.tempImageDir(file2Process.getFileId());
            Path imageDir = utilsService.imageDir(file2Process.getFileId());

            try {
                if(imageDir.toFile().exists()) {
                    LOG.info("delete {}", imageDir);
                    Utils.deleteDirectory(imageDir);

                    if(tempDir.toFile().exists()) {
                        LOG.info("move {} to {}", tempDir, imageDir);
                        Files.move(tempDir, imageDir, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                LOG.error("ERROR moving dirs", e);
            }

            modifiedOrNewImages.stream()
                .forEach(i2p-> {
                    try {
                        i2p.updateUrl(utilsService.imageURL(authService.getUsernameFromContext(), i2p.getFileId(), i2p.getPageNumber()));
                    } catch (MalformedURLException e) {
                        LOG.error("failed to create url for fileid {} page {}", i2p.getFileId(), i2p.getPageNumber(), e);
                    }
                });

            // --------------------------------------
            // Call LLM
            // --------------------------------------
            List<CompletionResponse> listCompletionResponse = new ArrayList<>();
            for (Image2Process image2Process : modifiedOrNewImages) {
                URL imageUrl = image2Process.getUrl();

                CompletionResponse completionResponse = qwenService.analyzeImage(
                        file2Process.getFileId(),
                        imageUrl);
                completionResponse.setFile2Process(file2Process);
                completionResponse.setPageNumber(image2Process.getPageNumber());
                listCompletionResponse.add(completionResponse);
                if (completionResponse.isCompleted()) {
                    LOG.info("FileId LLM OCR {} Image {} transcript length {}", file2Process.getFileId(), imageUrl, completionResponse.getTranscript().length());
                    processService.updateProcess(processId, "llm ocr done fileId " + file2Process.getFileId() + " page " + image2Process.getPageNumber());
                } else {
                    LOG.warn("FileId LLM OCR {} Image {} failed", file2Process.getFileId(), imageUrl);
                    processService.updateProcess(processId, "llm ocr failed fileId " + file2Process.getFileId() + " page " + image2Process.getPageNumber());
                }
            }

            // --------------------------------------
            // Create transcript db entities
            // --------------------------------------
            saveTranscript(file2Process.getFileId(), listCompletionResponse, listImages.size());

            // --------------------------------------
            // Create transcript page db entities
            // --------------------------------------
            saveTranscriptPages(file2Process.getFileId(), listCompletionResponse, modifiedOrNewImages);

            // --------------------------------------
            // Identify named entities
            // --------------------------------------
            namedEntitiesService.saveNamedEntities(file2Process.getFileId(), listCompletionResponse);
        }
        createFileEntities(files2Process);

        //processService.processDebug();

        LOG.info("Done processing files {}", files2Process.size());
    }

    private String saveTranscript(String fileId, List<CompletionResponse> listCompletionResponse, int transcriptTotalPageCount) {

        Optional<EntityTranscript> optDoc = repositoryTranscript.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));
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

        //TODO retrieve f2p is not very elegant
        if(!listCompletionResponse.isEmpty()) {
            File2Process f2p = listCompletionResponse.get(0).getFile2Process();
            entityTranscript
                    .setName(f2p.getFileName())
                    .setTranscripted_at(OffsetDateTime.now())
                    .setDocumented_at(TranscriptUtils.identifyDates(f2p))
                    .setPageCount(transcriptTotalPageCount);
            repositoryTranscript.save(entityTranscript);
        }
        return fileId;
    }

    private void saveTranscriptPages(String fileId, List<CompletionResponse> listCompletionResponse, List<Image2Process> modifiedOrNewImages) {
        //Map pageNumber -> CompletionResponse
        Map<Integer, CompletionResponse> mapPageNumber2Completion = listCompletionResponse.stream()
                .collect(Collectors.toMap(
                        CompletionResponse::getPageNumber,
                        cr -> cr
                ));

        for(Image2Process image2Process : modifiedOrNewImages) {
            CompletionResponse completionResponse = mapPageNumber2Completion.get(image2Process.getPageNumber());

            IdTranscriptPage idTranscriptPage = IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), fileId, completionResponse.getPageNumber());
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
                //TODO process transcript text
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
    }

    @NotNull
    private Images2Process getModifiedOrNewImages2Process(File2Process file2Process, List<URL> newImages) {
        String fileId = file2Process.getFileId();
        boolean allowCompare = true;

        // TODO do not retrieve when forced or disallowed compare
        // previous images
        Optional<EntityTranscript> transcript = repositoryTranscript.findById(IdFile.createIdFile(authService.getUsernameFromContext(), file2Process.getFileId()));
        List<BufferedImage> previousImages = new ArrayList<>();
        if(transcript.isPresent()) {
            for(int imageNum = 0; imageNum < transcript.get().getPageCount(); imageNum++) {
                Path imgPath = utilsService.imagePath(authService.getUsernameFromContext(), file2Process.getFileId(), imageNum);
                try {
                    previousImages.add(ImageIO.read(imgPath.toFile()));
                } catch (IOException e) {
                    LOG.error("Failed to load previous image from path path {}", imgPath);
                    allowCompare = false;
                }
            }
        }

        //compare with new images
        List<Image2Process> modifiedOrNewImages = new ArrayList<>();
        int changeAfter = 0;
        if(allowCompare && file2Process.isForce() == false && transcript.isPresent()) {
            boolean isFileModified = false;
            if(previousImages.size() == newImages.size()) {
                //same page count, we compare all images
                for (int imageNum = 0; imageNum < previousImages.size(); imageNum++) {
                    try {
                        BufferedImage previousImg = ImageIO.read(utilsService.imagePath(authService.getUsernameFromContext(), fileId, imageNum).toFile());
                        BufferedImage newImg = ImageIO.read(utilsService.tempImagePath(authService.getUsernameFromContext(), fileId, imageNum).toFile());
                        double comp = imageService.compareImages(previousImg, newImg);
                        if (Double.POSITIVE_INFINITY != comp) {
                            modifiedOrNewImages.add(Image2Process.create(fileId, imageNum, newImages.get(imageNum)));
                        }
                    } catch (IOException e) {
                        LOG.error("Failed to compare images img {} page {}", fileId, imageNum);
                        modifiedOrNewImages.add(Image2Process.create(fileId, imageNum, newImages.get(imageNum)));
                    }
                }
            } else {
                //compare until differs within the size of previous list -> we stop comparison at first diff
                int imageNum = 0;
                for (; imageNum < previousImages.size() && isFileModified == false; imageNum++) {
                    try {
                        BufferedImage previousImg = ImageIO.read(utilsService.imagePath(authService.getUsernameFromContext(), fileId, imageNum).toFile());
                        BufferedImage newImg = ImageIO.read(utilsService.tempImagePath(authService.getUsernameFromContext(), fileId, imageNum).toFile());
                        double comp = imageService.compareImages(previousImg, newImg);
                        if (Double.POSITIVE_INFINITY != comp) {
                            isFileModified = true;
                            modifiedOrNewImages.add(Image2Process.create(fileId, imageNum, newImages.get(imageNum)));
                        }
                    } catch (IOException e) {
                        LOG.error("Failed to compare images img {} page {}", fileId, imageNum);
                        isFileModified = true;
                        modifiedOrNewImages.add(Image2Process.create(fileId, imageNum, newImages.get(imageNum)));
                    }
                }

                //differs at imageNum
                changeAfter = imageNum;
                for (; imageNum < newImages.size(); imageNum++) {
                    modifiedOrNewImages.add(Image2Process.create(file2Process.getFileId(), imageNum, newImages.get(imageNum)));
                }
            }
        } else {
            //add all images to modified set
            for(int imageNum = 0; imageNum < newImages.size(); imageNum++) {
                modifiedOrNewImages.add(Image2Process.create(file2Process.getFileId(), imageNum, newImages.get(imageNum)));
            }
        }
        return new Images2Process()
                .setListImage2Process(modifiedOrNewImages)
                .setChangeAfterPageNumber(changeAfter);
    }

    @NotNull
    private List<EntityFile> createFileEntities(List<File2Process> files2Process) {
        List<EntityFile> listDocs = files2Process.stream()
                .map(f2p -> {
                    String fileId = f2p.getFileId();

                    Optional<EntityFile> optDoc = repositoryFile.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));
                    if (optDoc.isPresent()) {
                        return optDoc.get()
                                //updapte md5
                                .setMd5(f2p.getMd5());
                    } else {
                        return f2p.asEntity(authService.getUsernameFromContext());
                    }
                })
                .toList();

        //TODO why do this after processing ? if processing fails, all files cannot be registered
        repositoryFile.saveAll(listDocs);
        return listDocs;
    }

    private String updateAncestorsMonkeyFolders(String path) {
        Optional<EntityFile> optRootFolder = repositoryFile.findByIdFile_UsernameAndNameAndTypeIs(authService.getUsernameFromContext(), ROOT_FOLDER, FileType.folder);
        EntityFile rootFolder = null;
        if(optRootFolder.isPresent() == false) {
            EntityMonkeyFile rootEntityMonkeyFile = utilsService.createMonkeyFile(ROOT_FOLDER);
            rootFolder = new EntityFile()
                    .setIdFile(IdFile.createIdFile(authService.getUsernameFromContext(), rootEntityMonkeyFile.getId()))
                    .setType(FileType.folder)
                    .setName(ROOT_FOLDER);
            repositoryFile.save(rootFolder);

            preferencesService.setInputFolderId(rootEntityMonkeyFile.getId());

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
            EntityMonkeyFile emf = findOrCreateMonkeyFile(currentFolderPath);
            EntityFile entityFileFolder = findOrCreateFolder(emf, parentId);
            parentId = entityFileFolder.getIdFile().getFileId();

        }

        return parentId;

    }


    private EntityFile findOrCreateFolder(EntityMonkeyFile entityMonkeyFolder, String parentId) {

        IdFile idFile = IdFile.createIdFile(authService.getUsernameFromContext(), entityMonkeyFolder.getId());

        //TODO force folder type ?
        Optional<EntityFile> optFolder = repositoryFile.findById(idFile);

        if(optFolder.isPresent()) {
            return optFolder.get();
        } else {

            EntityFile folder = new EntityFile()
                    .setIdFile(idFile)
                    .setParentFolderId(parentId)
                    .setType(FileType.folder)
                    .setName(entityMonkeyFolder.getPath());

            return repositoryFile.save(folder);
        }
    }


    private EntityMonkeyFile findOrCreateMonkeyFile(String path) {

        //TODO add username
        String mFileId = utilsService.createMonkeySyncId(path);

        Optional<EntityMonkeyFile> optionalEntityMonkeyFile = repositoryMonkeyFile.findById(mFileId);
        if(optionalEntityMonkeyFile.isPresent()) {
            return optionalEntityMonkeyFile.get();
        } else {
            return repositoryMonkeyFile.save(new EntityMonkeyFile(mFileId, path));
        }
    }



    private String updateAncestorsFoldersGDrive(File2Process f2p) throws ServiceException {

        if(f2p.isLegacy() == false) {
            throw new ServiceException("Ancestors folders update only applies to legacy files");
        }

        String inboundFolderId = "";
        try {
            inboundFolderId = preferencesService.getPreference(PreferenceKey.inputFolderId);
        } catch (ServiceException e) {
            LOG.error("Failed to retrieve inbound folder id {}", inboundFolderId, e);
            return null;
        }

        File file = driveUtilsService.getDriveFileDetails(f2p.getFileId());
        List<File> ancestors = driveUtilsService.getAncestorsUntil(file, inboundFolderId, ANCESTORS_RETRIEVE_MAX_DEPTH,null);

        Collections.reverse(ancestors);

        List<EntityFile> folders = new ArrayList<>();
        EntityFile rootFolder = repositoryFile.findByIdFile_UsernameAndNameAndTypeIs(authService.getUsernameFromContext(), ROOT_FOLDER, FileType.folder)
                .orElse(new EntityFile()
                        .setIdFile(IdFile.createIdFile(authService.getUsernameFromContext(), inboundFolderId))
                        .setType(FileType.folder)
                        .setName(ROOT_FOLDER));
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

        //TODO try to save even already exists ?
        repositoryFile.saveAll(folders);

        return ROOT_FOLDER + ancestors.stream().map(File::getName).collect(Collectors.joining(ROOT_FOLDER));
    }

    @Override
    public void updateFolder(String folderId) {
        LOG.info("Prepare Async (Update folder id {})", folderId);

        Optional<Authentication> optAuth = authService.getLoggedAuthentication();
        if(optAuth.isEmpty()) {
            LOG.error("No authentication found");
            return;
        }

        SupplyAsyncAuthenticated sa = new SupplyAsyncAuthenticated(monitoringService, monitoringService.getCurrentMonitoringData(),
                optAuth.get(),
                () -> asyncUpdateFolder(folderId));
        processService.registerSyncProcess(authService.getUsernameFromContext(), AsyncProcessName.updateFolder, monitoringService.getCurrentMonitoringData(), "folder " + utilsService.getLocalFileName(folderId));
        CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

        processService.registerSyncProcessFuture(monitoringService.getCurrentMonitoringData(), future);

    }

    public void updateAll() {
        try {
            updateFolder(preferencesService.getPreference(PreferenceKey.inputFolderId));
        } catch (ServiceException e) {
            LOG.error("Failed to retrieve preferences", e);
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


        //TODO update process

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
                    recursRefreshFolder(file.getId(), offset + " ", max_depth - 1, currentFolderPath + ROOT_FOLDER + file.getName(), file.getName(), remoteFiles);

                } else {
                    LOG.info(offset + "{} ({})" ,file.getName(), file.getMd5Checksum());
                    Optional<EntityFile> optDoc = repositoryFile.findById(IdFile.createIdFile(authService.getUsernameFromContext(), file.getId()));
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
            String desc = new StringBuilder()
                    .append("forced update file ")
                    .append(utilsService.getLocalFileName(fileId)).append(" page ")
                    .append(pageNumber).toString();
            processService.registerSyncProcess(authService.getUsernameFromContext(), AsyncProcessName.forcePageUpdate, monitoringService.getCurrentMonitoringData(), desc);

            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);
            processService.registerSyncProcessFuture(monitoringService.getCurrentMonitoringData(), future);

        } catch (MalformedURLException e) {
            LOG.error("Failed to create image url {}", fileId, e);
        }
    }


    @MonitoringAsync
    private void asyncForcePageUpdate(String fileId, int pageNumber, URL imageURL) {
        LOG.info("Processing : Force page update for {} page {}", fileId, pageNumber);
        CompletionResponse completionResponse = qwenService.analyzeImage(fileId, imageURL);

        //TODO update process

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

            Optional<EntityTranscript> entityTranscript = repositoryTranscript.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));
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

        SupplyAsyncAuthenticated sa = new SupplyAsyncAuthenticated(monitoringService, monitoringService.getCurrentMonitoringData(),
                optAuth.get(),
                () -> asyncForceTranscriptUpdate(fileId));
        processService.registerSyncProcess(authService.getUsernameFromContext(), AsyncProcessName.forceTranscriptUpdate, monitoringService.getCurrentMonitoringData(),
                "update transcript " + utilsService.getLocalFileName(fileId));
        CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

        processService.registerSyncProcessFuture(monitoringService.getCurrentMonitoringData(), future);

    }

    @MonitoringAsync
    private void asyncForceTranscriptUpdate(String fileId) {

        //TODO update process

        try {
            File file = driveUtilsService.getDriveFileDetails(fileId);

            File fileParent = driveUtilsService.getDriveFileDetails(file.getParents().get(0));

            Path downloadFileFromDrive = driveUtilsService.downloadFileFromDrive(fileId, file.getName(), utilsService.downloadDir(fileId));


            //TODO why setFilePath ... after constructor (already doing the samed thing ??)
            File2Process file2Process = new File2Process(file)
                    .setFilePath(downloadFileFromDrive)
                    .setParentFolderId(fileParent.getId())
                    .setParentFolderName(fileParent.getName())
                    .setForce(true);

            runListAsyncProcess(List.of(file2Process));
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SyncEventResponse monkeySyncUpdate(MonkeyFileEvent monkeyFileEvent) {

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(monkeyFileEvent.getContent());

        byte[] hash = null;
        String md5;
        try {
            hash = MessageDigest.getInstance("MD5").digest(bytes);
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

        EntityMonkeyFile monkeyFile = utilsService.createMonkeyFile(virtualPath);
        String monkeyFolderId = updateAncestorsMonkeyFolders(basePath);

        Path downloadDir = utilsService.downloadDir(monkeyFolderId );
        Path targetFilePath = Paths.get(downloadDir.toString(), monkeyFile.getId());

        LOG.info("Write {} msId {} to {}", filename, monkeyFile.getId(), targetFilePath);
        try {
            Files.write(targetFilePath, bytes);
        } catch (IOException e) {
            LOG.error("Failed to write file", e);
        }

        File2Process f2p = new File2Process()
            .setFileId(monkeyFile.getId())
            .setFileName(filename)
            .setFilePath(targetFilePath)
            .setParentFolderId(monkeyFolderId)
            .setMd5(md5)
            .setMimeType(MIME_PDF);

            f2p.setFile2ProcessType(File2Process.File2ProcessType.monkeySync);

            String username = authService.getUsernameFromContext();

            if (mapScheduled.containsKey(username)) {
                mapScheduled.get(username).add(f2p);
            } else {
                mapScheduled.put(username, new ArrayList<>());
                mapScheduled.get(username).add(f2p);
            }

            return SyncEventResponse.acceptedSyncEventResponse(monkeyFile.getId());
        }


    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    //private Set<File2Process> setFiles2Process = Collections.synchronizedSet(new HashSet<>());
    private ConcurrentHashMap<String, List<File2Process>> mapScheduled = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext ctx;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        taskScheduler.scheduleWithFixedDelay(new FlushMonkeySyncTask(ctx), Duration.ofSeconds(30));

    }

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
            processService.registerSyncProcess(authService.getUsernameFromContext(), AsyncProcessName.flushMonkeySyncs, monitoringService.getCurrentMonitoringData(),
                    "flush monkey syncs (" + list.size() + " files)");
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            processService.registerSyncProcessFuture(monitoringService.getCurrentMonitoringData(), future);
        }
    }

    public void runListAsyncProcessForUser(List<File2Process> files2Process, String username) {
        NoAuthContextHolder.setContext(new NoAuthContext(username));
        runListAsyncProcess(files2Process);
    }




}
