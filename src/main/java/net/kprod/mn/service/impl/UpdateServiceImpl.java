package net.kprod.mn.service.impl;

import com.google.api.services.drive.model.File;
import net.kprod.mn.ServiceException;
import net.kprod.mn.data.repository.RepositoryNamedEntity;
import net.kprod.mn.transcript.NamedEntity;
import net.kprod.mn.utils.TranscriptUtils;
import net.kprod.mn.utils.Utils;
import net.kprod.mn.data.*;
import net.kprod.mn.data.entity.*;
import net.kprod.mn.data.enums.AsyncProcessName;
import net.kprod.mn.data.enums.FileType;
import net.kprod.mn.data.repository.RepositoryFile;
import net.kprod.mn.data.repository.RepositoryTranscript;
import net.kprod.mn.data.repository.RepositoryTranscriptPage;
import net.kprod.mn.monitoring.AsyncResult;
import net.kprod.mn.monitoring.MonitoringAsync;
import net.kprod.mn.monitoring.MonitoringService;
import net.kprod.mn.monitoring.SupplyAsyncAuthenticated;
import net.kprod.mn.service.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class UpdateServiceImpl implements UpdateService {
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
    private RepositoryNamedEntity repositoryNamedEntity;

    public void runListAsyncProcess(List<File2Process> files2Process) {

        for(File2Process file2Process : files2Process) {

            // --------------------------------------
            // get full path of the current file
            // --------------------------------------
            try {
                updateAncestorsFolders(file2Process.getFileId());
            } catch (ServiceException e) {
                LOG.warn("Failed to get full folder path {}", file2Process.getFileId(), e);
            }

            // --------------------------------------
            // PDF 2 images / temp dir
            // --------------------------------------
            List<URL> listImages = pdfService.pdf2Images(
                    authService.getUsernameFromContext(),
                    file2Process.getFileId(),
                    file2Process.getFilePath().toFile());
            LOG.info("PDF fileId {} file {} image list {}", file2Process.getFileId(), file2Process.getFilePath(), listImages.size());

            // --------------------------------------
            // Get list of modified or created images
            // --------------------------------------
            Images2Process images2Process = getModifiedOrNewImages2Process(file2Process, listImages);
            List<Image2Process> modifiedOrNewImages = images2Process.getListImage2Process();
            LOG.info("PDF fileId {} has {} modified or new pages", file2Process.getFileId(), modifiedOrNewImages.size());

            if(modifiedOrNewImages.isEmpty()) {
                LOG.info("No new or modified images, exiting from update process");
                return;
            }

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
                    LOG.info("FileId {} Image {} transcript length {}", file2Process.getFileId(), imageUrl, completionResponse.getTranscript().length());
                } else {
                    LOG.info("FileId {} Image {} failed", file2Process.getFileId(), imageUrl);
                }
            }
            LOG.info("Images converted {}", listCompletionResponse.size());

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

            // --------------------------------------
            // Identify named entities
            // --------------------------------------

            try {


                List<EntityNamedEntity> namedEntities = new ArrayList<>();
                for (CompletionResponse completionResponse : listCompletionResponse) {
                    //remove namedEntities associated to this page
                    repositoryNamedEntity.deleteByIdNamedEntity(authService.getUsernameFromContext(), completionResponse.getFileId(), completionResponse.getPageNumber());

                    List<NamedEntity> list = TranscriptUtils.identifyCommands(completionResponse.getTranscript());
                    for (NamedEntity namedEntity : list) {
                        LOG.info("Pages {} command {}", completionResponse.getPageNumber(), namedEntity);
                        IdNamedEntity idNamedEntity = IdNamedEntity.createIdNamedEntity(authService.getUsernameFromContext(), file2Process.getFileId(), completionResponse.getPageNumber());
                        namedEntities.add(new EntityNamedEntity()
                                .setIdNamedEntity(idNamedEntity)
                                .setVerb(namedEntity.getVerb())
                                .setValue(namedEntity.getValue())
                                .setStartIndex(namedEntity.getStart())
                                .setEndIndex(namedEntity.getEnd()));
                    }
                }
                repositoryNamedEntity.saveAll(namedEntities);
            }catch (Exception e) {
                LOG.error("ERROR updating images", e);
            }

            // --------------------------------------
            // Create transcript db entities
            // --------------------------------------
            saveTranscript(file2Process.getFileId(), listCompletionResponse, listImages.size());

            // --------------------------------------
            // Create transcript page db entities
            // --------------------------------------
            saveTranscriptPages(file2Process.getFileId(), listCompletionResponse, modifiedOrNewImages);
        }
        createFileEntities(files2Process);
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
        if(allowCompare && transcript.isPresent()) {
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

        //TODO do this after processing ?
        repositoryFile.saveAll(listDocs);
        return listDocs;
    }

    private String updateAncestorsFolders(String fileId) throws ServiceException {

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
        CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

        future.thenAccept(result -> {
           if(result.isSuccessful()) {
               LOG.info("Successfully updated folder id {}", folderId);
           } else if (result.isFailure()) {
               LOG.error("Failed to update folder id {}", folderId, result.getException());
           }
        });

        processService.registerSyncProcess(AsyncProcessName.updateFolder, monitoringService.getCurrentMonitoringData(), "folder " + utilsService.getLocalFileName(folderId), future);

    }

    public void updateAll() {
        try {
            updateFolder(preferencesService.getInputFolderId());
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
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            future.thenAccept(result -> {
                if(result.isSuccessful()) {
                    LOG.info("Successfully updated file id {} page {}", fileId, pageNumber);
                } else if (result.isFailure()) {
                    LOG.error("Failed to update file id {} page {}", fileId, pageNumber, result.getException());
                }
            });

            String desc = new StringBuilder()
                    .append("forced update file ")
                    .append(utilsService.getLocalFileName(fileId)).append(" page ")
                    .append(pageNumber).toString();
            processService.registerSyncProcess(AsyncProcessName.forcePageUpdate, monitoringService.getCurrentMonitoringData(), desc, future);

        } catch (MalformedURLException e) {
            LOG.error("Failed to create image url {}", fileId, e);
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
        CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

        future.thenAccept(result -> {
            if(result.isSuccessful()) {
                LOG.info("Successfully updated file id {}", fileId);
            } else if (result.isFailure()) {
                LOG.error("Failed to update file id {}", fileId, result.getException());
            }
        });


        processService.registerSyncProcess(AsyncProcessName.forceTranscriptUpdate, monitoringService.getCurrentMonitoringData(),
                "update transcript " + utilsService.getLocalFileName(fileId), future);

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
}
