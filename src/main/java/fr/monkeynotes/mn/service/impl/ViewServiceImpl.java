package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.ViewOptions;
import fr.monkeynotes.mn.data.dto.*;
import fr.monkeynotes.mn.data.entity.*;
import fr.monkeynotes.mn.data.enums.FileType;
import fr.monkeynotes.mn.data.enums.NamedEntityVerb;
import fr.monkeynotes.mn.data.enums.ViewOptionsCompletionStatus;
import fr.monkeynotes.mn.data.repository.RepositoryFile;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntity;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPage;
import fr.monkeynotes.mn.service.*;
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
    private RepositoryTranscriptPage repositoryTranscriptPage;

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

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    private IdFile idFile(String fileId) {
        return IdFile.createIdFile(authService.getUsernameFromContext(), fileId);
    }

    @Override
    public DtoTranscript getTranscript(String fileId, ViewOptions viewOptions) {
        IdFile idFile = idFile(fileId);

        Optional<EntityTranscript> optDoc = repositoryTranscript.findById(idFile);
        Optional<EntityFile> optFile = repositoryFile.findById(idFile);
        if (optDoc.isPresent() && optFile.isPresent()) {
            DtoTranscript dtoTranscript = buildDtoTranscript(optDoc.get(), optFile.get(), viewOptions);
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
        List<EntityFile> children = repositoryFile.findAllByIdFile_UsernameAndParentFolderId(authService.getUsernameFromContext(), directory.getFileId());
        for (EntityFile child : children) {
            DtoTranscript dtoTranscript = null;
            if (child.getType() == FileType.transcript) {
                Optional<EntityTranscript> optTranscript = repositoryTranscript.findById(child.getIdFile());
                if(optTranscript.isPresent()) {
                    dtoTranscript = buildDtoTranscript(optTranscript.get(), child, ViewOptions.all());
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
        List<EntityFile> childen = repositoryFile.findAllByIdFile_UsernameAndParentFolderId(authService.getUsernameFromContext(), directory.getFileId());
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
            LOG.error("Failed to list root level", e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<FileNode> listLevel(String folderId) {
        return repositoryFile.findAllByIdFile_UsernameAndParentFolderId(authService.getUsernameFromContext(), folderId).stream()
                .map(f -> {
                    FileNode node = new FileNode(DtoFile.fromEntity(f));
                    Optional<EntityTranscript> optTranscript = repositoryTranscript.findById(f.getIdFile());
                    if(optTranscript.isPresent()) {
                        node.setDtoTranscript(buildDtoTranscript(optTranscript.get(), f, ViewOptions.all()));
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

                    return buildDtoTranscript(t, f.get(), ViewOptions.all());
                })
                .toList();
    }

    @Override
    public String getContent(DtoTranscript dtoTranscript) throws ServiceException {
        if(dtoTranscript == null) {
            throw new ServiceException("dtoTranscript is null");
        }
        StringBuilder sbContent = new StringBuilder();

        for(int n = 0; n < dtoTranscript.getPageCount(); n++) {
            Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(
                    IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), dtoTranscript.getFileId(), n));

            if (optPage.isPresent()) {
                DtoTranscriptPage dtoTranscriptPage = DtoTranscriptPage.fromEntity(optPage.get());
                dtoTranscriptPage = editService.applyPatch(dtoTranscriptPage);
                sbContent.append(dtoTranscriptPage.getTranscript());
            }
        }
        return sbContent.toString();
    }

    private DtoTranscript buildDtoTranscript(EntityTranscript t, EntityFile file, ViewOptions viewOptions) {
        //List<Optional<EntityTranscriptPage>> listPages = new ArrayList<>();

        //todo optimize ? include in all requests ? // remove n ?
        //TODO replace optional ?

        List<DtoTranscriptPage> listDtoTranscriptPages = new ArrayList<>();

        Optional<String> optNextPageSchema = Optional.empty();
        for(int n = 0; n < t.getPageCount(); n++) {
            Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(
                    IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), t.getIdFile().getFileId(), n));

            if(optPage.isPresent()) {
                DtoTranscriptPage dtoTranscriptPage = DtoTranscriptPage.fromEntity(optPage.get());
                List<DtoNamedEntity> namedEntities = repositoryNamedEntity.findBy(authService.getUsernameFromContext(), t.getIdFile().getFileId(), n).stream()
                    .map(ne -> DtoNamedEntity.fromEntity(ne))
                        .sorted(Comparator.comparing(DtoNamedEntity::getStart))
                        .toList();
                dtoTranscriptPage.setListNamedEntities(namedEntities);


//                //schema ref from last page
//                dtoTranscriptPage.setOptSchemaTitle(optNextPageSchema);
//                optNextPageSchema = Optional.empty();
//
//                //schema ref for next page
//                optNextPageSchema = namedEntities.stream()
//                        .filter(ne->ne.getVerb().equals(NamedEntityVerb.refSchema))
//                        .map(DtoNamedEntity::getValue)
//                        .findFirst();
//
//                //schema for this page
//                Optional<String> optSchema = namedEntities.stream()
//                        .filter(ne->ne.getVerb().equals(NamedEntityVerb.schema))
//                        .map(DtoNamedEntity::getValue)
//                        .findFirst();
//                dtoTranscriptPage.setOptSchemaTitle(optSchema);

                dtoTranscriptPage = editService.applyPatch(dtoTranscriptPage);
                listDtoTranscriptPages.add(dtoTranscriptPage);
            }
        }

        DtoTranscript dtoTranscript = DtoTranscript.fromEntity(t, listDtoTranscriptPages);

        //List<DtoTranscriptPage> listP = dtoTranscript.getPages();

        listDtoTranscriptPages.stream()
            .map(page->{
                try {
                    page.setImageUrl(utilsService.imageURL(authService.getUsernameFromContext(), page.getFileId(), page.getPageNumber()));
                } catch (MalformedURLException e) {
                    LOG.error("Failed to create image URL fileId {} page {}", page.getFileId(), page.getPageNumber());
                }

                //page = editService.applyPatch(page);
                return page;
            })
            .toList();

        if (viewOptions.getCompletionStatus() == ViewOptionsCompletionStatus.failed) {
            listDtoTranscriptPages = listDtoTranscriptPages.stream()
                    .filter(p -> !p.isCompleted())
                    .toList();
        }

        dtoTranscript.setPages(listDtoTranscriptPages);
        //TODO this requires file entity ; is this really needed ?
        dtoTranscript.setDiscovered_at(file.getDiscovered_at());

        dtoTranscript.setTagsMap(dtoTranscript.getPages().stream()
                .flatMap(p -> p.getListNamedEntities().stream())
                .filter(ne->ne.getVerb().equals(NamedEntityVerb.tag))
                .collect(Collectors.groupingBy(DtoNamedEntity::getValue)));

        dtoTranscript.setToc(dtoTranscript.getPages().stream()
                .flatMap(p -> p.getListNamedEntities().stream())
                .filter(ne -> NamedEntityVerb.isToc(ne.getVerb()))
                .collect(Collectors.toList()));

        return dtoTranscript;
    }

    @Autowired
    private EditService editService;


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
