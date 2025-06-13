package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.ViewOptions;
import net.kprod.dsb.data.dto.*;
import net.kprod.dsb.data.entity.*;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.enums.ViewOptionsCompletionStatus;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.data.repository.RepositoryTranscriptPage;
import net.kprod.dsb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ViewServiceImpl implements ViewService {
    Logger LOG = LoggerFactory.getLogger(ViewServiceImpl.class);

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PreferencesService preferencesService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    private IdFile idFile(String fileId) {
        return IdFile.createIdFile(authService.getUsernameFromContext(), fileId);
    }

//    @Override
//    public List<String> listAvailableTranscripts() {
//        return repositoryTranscript.findAll().stream() //optimize request
//                .filter(d -> d.getTranscripted_at() != null)
//                .map(d -> {
//                    return new StringBuilder()
//                            //.append(d.getFileId()).append(" - ").append(d.getName())
//                            .append(d.getIdFile().getFileId()).append(" - ").append(d.getName())
//                            .toString();
//                })
//                .toList();
//    }

    @Override
    public DtoTranscript getTranscript(String fileId, ViewOptions viewOptions) {
        IdFile idFile = idFile(fileId);

        Optional<EntityTranscript> optDoc = repositoryTranscript.findById(idFile);
        Optional<EntityFile> optFile = repositoryFile.findById(idFile);
        if (optDoc.isPresent() && optFile.isPresent()) {
            DtoTranscript dtoTranscript = buildDtoTranscript(optDoc.get(), viewOptions);
            return dtoTranscript;
        }
        //todo error
        return null;
    }

    @Override
    public List<DtoTranscriptDetails> listRecentTranscripts(int from, int to) {
        List<EntityTranscript> list = repositoryTranscript.findRecentByIdFile_Username(authService.getUsernameFromContext(), PageRequest.of(from,to));
        List<DtoTranscriptDetails> listDtoRecent = new ArrayList<>();
        for (EntityTranscript entityTranscript : list) {
            DtoTranscriptDetails dtoTranscriptDetails = null;
            List<String> parents = driveUtilsService.getDriveParents(entityTranscript.getIdFile().getFileId());
            if(parents.isEmpty() == false) {
                IdFile parentIdFile = IdFile.createIdFile(authService.getUsernameFromContext(), parents.get(0));
                Optional<EntityFile> parentFile = repositoryFile.findById(parentIdFile);
                if(parentFile.isPresent()) {
                    dtoTranscriptDetails = new DtoTranscriptDetails(
                            DtoTranscript.fromEntity(entityTranscript),
                            DtoFile.fromEntity(parentFile.get()));

                    listDtoRecent.add(dtoTranscriptDetails);
                }
            }
        }
        return listDtoRecent;
    }

    private List<FileNode> listFileNodesRecurs(EntityFile dir) {
        DtoFile directory = DtoFile.fromEntity(dir);
        List<FileNode> fileNodes = new ArrayList<>();
        List<EntityFile> children = repositoryFile.findAllByParentFolderId(directory.getFileId());
        for (EntityFile child : children) {
            DtoTranscript dtoTranscript = null;
            if (child.getType() == FileType.transcript) {
                Optional<EntityTranscript> optTranscript = repositoryTranscript.findById(child.getIdFile());
                if(optTranscript.isPresent()) {
                    dtoTranscript = buildDtoTranscript(optTranscript.get(), ViewOptions.all());
                } else {
                    LOG.warn("No transcript found for id {}", child.getIdFile());
                    //todo NO transcript / error
                }
            }
            FileNode node = new FileNode(DtoFile.fromEntity(child));
            node.setDtoTranscript(dtoTranscript);
            if (child.getType() == FileType.folder) {
                node.setChildren(listFileNodesRecurs(child));
            } //HERE
            fileNodes.add(node);
        }
        return fileNodes;
    }

    private List<DtoFile> listAllFilesRecurs(EntityFile dir) {
        DtoFile directory = DtoFile.fromEntity(dir);
        List<DtoFile> files = new ArrayList<>();
        List<EntityFile> childen = repositoryFile.findAllByParentFolderId(directory.getFileId());
        for (EntityFile child : childen) {
            if (child.getType() == FileType.transcript) {
                files.add(DtoFile.fromEntity(child));
            } if(child.getType() == FileType.folder) {
                files.addAll(listAllFilesRecurs(child));
            }
        }
        return files;
    }

    EntityFile getRootFolder() throws ServiceException {
        String inboundFolderId = preferencesService.getInputFolderId();

        Optional<EntityFile> optFolder = repositoryFile.findById(idFile(inboundFolderId));
        if(optFolder.isEmpty()) {
            throw new ServiceException("Folder not found id " + inboundFolderId);
        }

        return optFolder.get();
    }

    @Override
    public List<FileNode> listAllNodes() {
        try {
            return listFileNodesRecurs(getRootFolder());
        } catch (ServiceException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<FileNode> listRootLevel() {
        try {
            return listLevel(getRootFolder().getIdFile().getFileId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FileNode> listLevel(String folderId) {
        return repositoryFile.findAllByParentFolderId(folderId).stream()
                .map(f -> {
                    FileNode node = new FileNode(DtoFile.fromEntity(f));

                    Optional<EntityTranscript> optTranscript = repositoryTranscript.findById(f.getIdFile());
                    if(optTranscript.isPresent()) {

                        node.setDtoTranscript(buildDtoTranscript(optTranscript.get(), ViewOptions.all()));
                    }
                    return node;
                })
                .sorted(Comparator.comparing(fileNode -> fileNode.getName()))
                .toList();
    }

    @Override
    public List<DtoTranscript> listTranscriptFromFolderRecurs (String folderId) {
        Optional<EntityFile> optFolder = repositoryFile.findById(idFile(folderId));

        if(!optFolder.isPresent()) {
            return Collections.emptyList();
        }

        Set<IdFile> setTranscriptId = listAllFilesRecurs(optFolder.get()).stream()
                .map(d -> {
                    return IdFile.createIdFile(authService.getUsernameFromContext(), d.getFileId());
                })
                .collect(Collectors.toSet());

        return repositoryTranscript.findAllByIdFileIn(setTranscriptId).stream()
                .map(t -> {

                    Optional<EntityFile> f = repositoryFile.findById(t.getIdFile());

                    //String parentFolderId = f.isPresent() ? f.get().getParentFolderId() : folderId;

                    return buildDtoTranscript(t, ViewOptions.all());
                })
                .toList();
    }

    private DtoTranscript buildDtoTranscript(EntityTranscript t, ViewOptions viewOptions) {
        List<Optional<EntityTranscriptPage>> listPages = new ArrayList<>();

        for(int n = 1; n <= t.getPageCount(); n++) {
            //todo optimize ? include in all requests ? // remove n ?
            Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(
                    IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), t.getIdFile().getFileId(), n));
                        listPages.add(optPage);
        }

        DtoTranscript dtoTranscript = DtoTranscript.fromEntity(t,
                listPages.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList());

        List<DtoTranscriptPage> listP = dtoTranscript.getPages();

        listP.stream()
            .map(page->{
                try {
                    page.setImageUrl(utilsService.imageURL(authService.getUsernameFromContext(), page.getFileId(), page.getPageNumber()));
                } catch (MalformedURLException e) {
                    LOG.error("Failed to create image URL fileId {} page {}", page.getFileId(), page.getPageNumber());
                }

                String[] lines = page.getTranscript().split("\n");
                StringBuilder sb = new StringBuilder();
                for(String line : lines) {
                    sb.append(line).append("<br/>");
                }
                page.setTranscriptHtml(sb.toString());
                return page;
            })
            .toList();

        if (viewOptions.getCompletionStatus() == ViewOptionsCompletionStatus.failed) {
            listP = listP.stream()
                    .filter(p -> !p.isCompleted())
                    .toList();
        }

        dtoTranscript.setPages(listP);
        return dtoTranscript;
    }

    @Override
    public File createTranscriptPdf(String fileId) throws IOException {
        return pdfService.createTranscriptPdf(fileId, Collections.singletonList(getTranscript(fileId, ViewOptions.all())));
    }

    @Override
    public File createTranscriptPdfFromFolder(String folderId) throws IOException {
        return pdfService.createTranscriptPdf(folderId, listTranscriptFromFolderRecurs(folderId));
    }

    //delete transcript or folder
    @Override
    @Transactional
    public void delete(String fileId) {

        Optional<EntityFile> t = repositoryFile.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));

        if(t.isEmpty()) {
            LOG.error("No file found for id {}", fileId);
            return;
        }

        EntityFile entityFile = t.get();
        if(entityFile.getType().equals(FileType.folder)) {
            //folder
            List<DtoTranscript> listTranscripts = listTranscriptFromFolderRecurs(fileId);
            LOG.info("Delete {} items from database", listTranscripts.size());
            for(DtoTranscript dtoTranscript : listTranscripts) {
                deleteFromDb(dtoTranscript.getFileId(), false);
            }
            deleteFromDb(fileId, true);
        } else  {
            deleteFromDb(fileId, false);
        }
    }

    private void deleteFromDb(String fileId, boolean isFolder) {
        LOG.info("Delete {} from database", fileId);
        IdFile idFile = IdFile.createIdFile(authService.getUsernameFromContext(), fileId);
        repositoryTranscript.deleteById(idFile);
        repositoryFile.deleteById(idFile);
        if(isFolder == false) {
            repositoryTranscriptPage.deleteByIdTranscriptPage_FileId(fileId);
        }
    }
}
