package net.kprod.dsb.service.impl;

import com.google.api.services.drive.model.File;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.Utils;
import net.kprod.dsb.data.CompletionResponse;
import net.kprod.dsb.data.DriveFileTypes;
import net.kprod.dsb.data.File2Process;
import net.kprod.dsb.data.entity.*;
import net.kprod.dsb.data.enums.AsyncProcessName;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.data.repository.RepositoryTranscriptPage;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringAsync;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.monitoring.SupplyAsyncAuthenticated;
import net.kprod.dsb.service.*;
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
import java.nio.file.Path;
import java.time.OffsetDateTime;
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

    public void runListAsyncProcess(List<File2Process> files2Process) {
        // create file objects
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
            boolean allowCompare = true;
            Optional<EntityTranscript> t = repositoryTranscript.findById(IdFile.createIdFile(authService.getUsernameFromContext(), file2Process.getFileId()));
            List<BufferedImage> previousImages = new ArrayList<>();
            if(t.isPresent()) {
                currentCount = t.get().getPageCount();
                for(int imageNum = 1; imageNum <= t.get().getPageCount(); imageNum++) {
                    Path imgPath = utilsService.imagePath(authService.getUsernameFromContext(), file2Process.getFileId(), imageNum);
                    try {
                        previousImages.add(ImageIO.read(imgPath.toFile()));
                    } catch (IOException e) {
                        LOG.error("Failed to load image fileid {} page {}", file2Process.getFileId(), imageNum);
                        allowCompare = false;
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
            if(listImages.size() == currentCount && allowCompare) {
                //same page count, compare content
                for(int imageNum = 1; imageNum <= listImages.size(); imageNum++) {
                    try {
                        BufferedImage img = ImageIO.read(utilsService.imagePath(authService.getUsernameFromContext(), file2Process.getFileId(), imageNum).toFile());
                        double comp = imageService.compareImages(previousImages.get(imageNum - 1), img);

//                        if(Double.POSITIVE_INFINITY == comp) {
//                            LOG.info("FileId {} page {} UNMODIFIED", file2Process.getFileId(), imageNum);
//                        } else {
//                            isFileModified = true;
//                            LOG.info("FileId {} page {} MODIFIED", file2Process.getFileId(), imageNum);
//                        }

                        if(Double.POSITIVE_INFINITY != comp) {
                            isFileModified = true;
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
                            .setDocumented_at(Utils.identifyDates(f2p))
                            .setPageCount(listCompletionResponse.size());

                    repositoryTranscript.save(entityTranscript);

                }
            }

        }
        LOG.info("Done processing files {}", listDocs.size());
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

        try {
            SupplyAsyncAuthenticated sa = new SupplyAsyncAuthenticated(monitoringService, monitoringService.getCurrentMonitoringData(),
                    optAuth.get(),
                    () -> asyncUpdateFolder(folderId));
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            processService.registerSyncProcess(AsyncProcessName.updateFolder, monitoringService.getCurrentMonitoringData(), "folder " + utilsService.getLocalFileName(folderId), future);
        } catch (ServiceException e) {
            LOG.info("Failed to prepare updateFolder async", e);
        }
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



            String desc = new StringBuilder()
                    .append("forced update file ")
                    .append(utilsService.getLocalFileName(fileId)).append(" page ")
                    .append(pageNumber).toString();
            processService.registerSyncProcess(AsyncProcessName.forcePageUpdate, monitoringService.getCurrentMonitoringData(), desc, future);

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

        try {
            SupplyAsyncAuthenticated sa = new SupplyAsyncAuthenticated(monitoringService, monitoringService.getCurrentMonitoringData(),
                    optAuth.get(),
                    () -> asyncForceTranscriptUpdate(fileId));
            CompletableFuture<AsyncResult> future = CompletableFuture.supplyAsync(sa);

            processService.registerSyncProcess(AsyncProcessName.forceTranscriptUpdate, monitoringService.getCurrentMonitoringData(),
                    "update transcript " + utilsService.getLocalFileName(fileId), future);
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
}
