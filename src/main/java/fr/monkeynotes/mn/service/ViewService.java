package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.ViewOptions;
import fr.monkeynotes.mn.data.dto.*;
import fr.monkeynotes.mn.data.entity.*;
import fr.monkeynotes.mn.data.enums.FileType;
import fr.monkeynotes.mn.data.enums.NamedEntityVerb;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.data.enums.ViewOptionsCompletionStatus;
import fr.monkeynotes.mn.data.repository.RepositoryFile;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntity;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPage;
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
public class ViewService {
    Logger LOG = LoggerFactory.getLogger(ViewService.class);

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

    //TODO make Specialized id objects

    public List<DtoTranscriptDetails> listRecentTranscripts(int from, int to) {
        return toTranscriptDetails(repositoryTranscript.findRecentByIdFile_Username(
                authService.getUsernameFromContext(), PageRequest.of(from, to)));
    }

    /**
     * Every transcript of the account, unordered — backs the home "by date" listing, which groups
     * the whole corpus by year client-side rather than paging through it.
     */
    public List<DtoTranscriptDetails> listAllTranscripts() {
        return toTranscriptDetails(repositoryTranscript.findAllByIdFile_Username(authService.getUsernameFromContext()));
    }

    /**
     * A transcript row carries no date of its own for the file-level "discovered" timestamp and no
     * parent name, so both come from the matching file rows here. A transcript whose file row is
     * gone is dropped; one whose parent folder is gone is kept with a null parent, so a document
     * orphaned by a half-finished delete still shows up in a listing.
     */
    private List<DtoTranscriptDetails> toTranscriptDetails(List<EntityTranscript> transcripts) {
        String username = authService.getUsernameFromContext();

        Map<IdFile, EntityFile> files = repositoryFile.findAllById(
                        transcripts.stream().map(EntityTranscript::getIdFile).toList()).stream()
                .collect(Collectors.toMap(EntityFile::getIdFile, f -> f));

        List<DtoTranscriptDetails> listDetails = new ArrayList<>();
        for (EntityTranscript entityTranscript : transcripts) {
            EntityFile file = files.get(entityTranscript.getIdFile());
            if (file == null) {
                LOG.warn("No file found for transcript {}", entityTranscript.getIdFile());
                continue;
            }

            DtoFile parent = Optional.ofNullable(file.getParentFolderId())
                    .flatMap(parentFolderId -> repositoryFile.findById(IdFile.createIdFile(username, parentFolderId)))
                    .map(DtoFile::fromEntity)
                    .orElse(null);

            listDetails.add(new DtoTranscriptDetails(
                    DtoTranscript.fromEntity(entityTranscript).setDiscovered_at(file.getDiscovered_at()),
                    parent));
        }
        return listDetails;
    }

    public DtoCounts countFiles() {
        String username = authService.getUsernameFromContext();
        return new DtoCounts()
                .setFolders(repositoryFile.countByIdFile_UsernameAndType(username, FileType.folder))
                .setTranscripts(repositoryFile.countByIdFile_UsernameAndType(username, FileType.transcript));
    }

    /**
     * The transcript half of a listing row, built exactly as the "by date" listing builds it
     * ({@link DtoTranscript#fromEntity(EntityTranscript)} plus the file row's discovered_at), and
     * deliberately NOT via {@link #buildDtoTranscript} — a listing only needs the title and the
     * dates, while buildDtoTranscript reads every page and every page's named entities and applies
     * every stored diff. That is several queries per page per document just to draw one row, and it
     * lets a single unappliable diff (EditService.applyPatch rethrows as an unchecked exception)
     * fail the whole folder listing instead of one transcript view.
     */
    private DtoTranscriptDetails transcriptDetails(EntityFile file, DtoFile parent) {
        Optional<EntityTranscript> optTranscript = repositoryTranscript.findById(file.getIdFile());
        if (optTranscript.isEmpty()) {
            LOG.warn("No transcript found for id {}", file.getIdFile());
            return null;
        }
        return new DtoTranscriptDetails(
                DtoTranscript.fromEntity(optTranscript.get()).setDiscovered_at(file.getDiscovered_at()),
                parent);
    }

    private List<FileNode> listFileNodesRecurs(EntityFile dir) {
        DtoFile directory = DtoFile.fromEntity(dir);
        List<FileNode> fileNodes = new ArrayList<>();
        List<EntityFile> children = repositoryFile.findAllByIdFile_UsernameAndParentFolderId(authService.getUsernameFromContext(), directory.getFileId());
        for (EntityFile child : children) {
            FileNode node = new FileNode(DtoFile.fromEntity(child));
            if (child.getType() == FileType.transcript) {
                node.setTranscriptDetails(transcriptDetails(child, directory));
            }
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

    /**
     * Empty whenever the account has no usable input folder: preferences never initialised, no
     * inputFolderId set yet (a fresh account that has never synced), or an id pointing at a folder
     * that is no longer in the database. None of those is a failure — the callers turn it into an
     * empty listing and the UI renders its "no documents yet" state — so this deliberately does not
     * throw. It used to, which logged an ERROR with a stack trace on every root listing for any
     * account that had not synced yet.
     */
    Optional<EntityFile> findRootFolder() {
        return preferencesService.getPreferenceOpt(PreferenceKey.inputFolderId)
                .filter(inboundFolderId -> inboundFolderId.isBlank() == false)
                .flatMap(inboundFolderId -> repositoryFile.findById(idFile(inboundFolderId)));
    }

    public List<FileNode> listAllNodes() {
        return findRootFolder()
                .map(rootFolder -> listFileNodesRecurs(rootFolder))
                .orElseGet(Collections::emptyList);
    }

    public List<FileNode> listRootLevel() {
        return findRootFolder()
                .map(rootFolder -> listLevel(rootFolder.getIdFile().getFileId()))
                .orElseGet(Collections::emptyList);
    }

    public List<FileNode> listLevel(String folderId) {
        //resolved once for the whole level rather than per child: every row here has the same parent
        DtoFile parent = repositoryFile.findById(idFile(folderId)).map(DtoFile::fromEntity).orElse(null);

        return repositoryFile.findAllByIdFile_UsernameAndParentFolderId(authService.getUsernameFromContext(), folderId).stream()
                .map(f -> {
                    FileNode node = new FileNode(DtoFile.fromEntity(f));
                    if (f.getType() == FileType.transcript) {
                        node.setTranscriptDetails(transcriptDetails(f, parent));
                    }
                    return node;
                })
                .sorted(Comparator.comparing(fileNode -> fileNode.getName()))
                .toList();
    }

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

//    public String getContent(DtoTranscript dtoTranscript) throws ServiceException {
//        if(dtoTranscript == null) {
//            throw new ServiceException("dtoTranscript is null");
//        }
//        StringBuilder sbContent = new StringBuilder();
//
//        for(int n = 0; n < dtoTranscript.getPageCount(); n++) {
//            Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(
//                    IdTranscriptPage.createIdTranscriptPage(authService.getUsernameFromContext(), dtoTranscript.getFileId(), n));
//
//            if (optPage.isPresent()) {
//                DtoTranscriptPage dtoTranscriptPage = DtoTranscriptPage.fromEntity(optPage.get());
//                dtoTranscriptPage = editService.applyPatch(dtoTranscriptPage);
//                sbContent.append(dtoTranscriptPage.getTranscript());
//            }
//        }
//        return sbContent.toString();
//    }

    private DtoTranscript buildDtoTranscript(EntityTranscript t, EntityFile file, ViewOptions viewOptions) {
        //todo optimize ? include in all requests ? // remove n ?
        //TODO replace optional ?

        List<DtoTranscriptPage> listDtoTranscriptPages = new ArrayList<>();

        Optional<String> optNextPageDiagramTitle = Optional.empty();
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

                //diagram for this page
                Optional<String> optDiagramTitle = namedEntities.stream()
                        .filter(ne->ne.getVerb().equals(NamedEntityVerb.diagram))
                        .map(DtoNamedEntity::getValue)
                        .findFirst();

                if(optDiagramTitle.isPresent()) {
                    dtoTranscriptPage.setDiagramTitle(optDiagramTitle.get());
                    dtoTranscriptPage.setPageDiagram(DtoTranscriptPage.PageDiagram.full);
                }
                else if (optNextPageDiagramTitle.isPresent()) {
                    dtoTranscriptPage.setDiagramTitle(optNextPageDiagramTitle.get());
                    dtoTranscriptPage.setPageDiagram(DtoTranscriptPage.PageDiagram.inline);
                } else {
                  //no diagram in this transcript
                    dtoTranscriptPage.setPageDiagram(DtoTranscriptPage.PageDiagram.none);
                }

                //diagram ref for next page
                optNextPageDiagramTitle = namedEntities.stream()
                    .filter(ne->{
                        return (ne.getVerb().equals(NamedEntityVerb.diagramNextPage) || ne.getVerb().equals(NamedEntityVerb.refSchema2_DEL));
                    })
                    .map(DtoNamedEntity::getValue)
                    .findFirst();

                dtoTranscriptPage = editService.applyPatch(dtoTranscriptPage);

                //LOG.info("page {} schema {} {}", dtoTranscriptPage.getPageNumber(), dtoTranscriptPage.isSchema() ? "YES" : "NO", dtoTranscriptPage.getSchemaTitle());

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


    public File createTranscriptPdf(String fileId) throws IOException {
        return pdfService.createTranscriptPdf(fileId, Collections.singletonList(getTranscript(fileId, ViewOptions.all())));
    }

    public File createTranscriptPdfFromFolder(String folderId) throws IOException {
        return pdfService.createTranscriptPdf(folderId, listTranscriptFromFolderRecurs(folderId));
    }

    //delete transcript or folder
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
