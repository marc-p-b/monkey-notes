package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoFile;
import net.kprod.dsb.data.dto.DtoTranscript;
import net.kprod.dsb.data.dto.DtoTranscriptPage;
import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.data.entity.*;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.data.repository.RepositoryTranscriptPage;
import net.kprod.dsb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private IdFile idFile(String fileId) {
        return IdFile.createIdFile(authService.getConnectedUsername(), fileId);
    }

    @Override
    public List<String> listAvailableTranscripts() {
        return repositoryTranscript.findAll().stream() //optimize request
                .filter(d -> d.getTranscripted_at() != null)
                .map(d -> {
                    return new StringBuilder()
                            //.append(d.getFileId()).append(" - ").append(d.getName())
                            .append(d.getIdFile().getFileId()).append(" - ").append(d.getName())
                            .toString();
                })
                .toList();
    }

    @Override
    public DtoTranscript getTranscript(String fileId) {
        IdFile idFile = idFile(fileId);

        Optional<EntityTranscript> optDoc = repositoryTranscript.findById(idFile);
        Optional<EntityFile> optFile = repositoryFile.findById(idFile);
        if (optDoc.isPresent() && optFile.isPresent()) {
            DtoTranscript dtoTranscript = buildDtoTranscript(optDoc.get(), optFile.get().getParentFolderId());
            return dtoTranscript;
        }
        //todo error
        return null;
    }

    @Autowired
    private UtilsService utilsService;

    private List<FileNode> listFileNodesRecurs(EntityFile dir) {
        DtoFile directory = DtoFile.fromEntity(dir);
        List<FileNode> fileNodes = new ArrayList<>();
        List<EntityFile> childen = repositoryFile.findAllByParentFolderId(directory.getFileId());
        for (EntityFile child : childen) {
            DtoTranscript dtoTranscript = null;
            if (child.getType() == FileType.transcript) {


                Optional<EntityTranscript> optTranscript = repositoryTranscript.findById(child.getIdFile());
                if(optTranscript.isPresent()) {
                    dtoTranscript = buildDtoTranscript(optTranscript.get(), dir.getParentFolderId());
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

    @Override
    public List<FileNode> listFolders() {

        String inboundFolderId = null;
        try {
            inboundFolderId = preferencesService.getInputFolderId();
        } catch (ServiceException e) {
            LOG.warn("inbound folder id not set", e);
            return Collections.emptyList();
        }

        List<FileNode> folders = null;
        try {
            EntityFile folder = repositoryFile.findById(idFile(inboundFolderId)).orElseThrow(() -> new ServiceException("inbound folder not found"));
            folders = listFileNodesRecurs(folder);
        } catch (ServiceException e) {
            LOG.error("Error listing folders", e);
            return Collections.emptyList();
        }

        // todo do this in recurs ? (does not work)
        return folders;
    }

    private List<DtoTranscript> listTranscriptFromFolderRecurs (String folderId) {
        Optional<EntityFile> optFolder = repositoryFile.findById(idFile(folderId));

        if(!optFolder.isPresent()) {
            return Collections.emptyList();
        }

        Set<IdFile> setTranscriptId = listAllFilesRecurs(optFolder.get()).stream()
                .map(d -> {
                    return IdFile.createIdFile(authService.getConnectedUsername(), d.getFileId());
                })
                .collect(Collectors.toSet());


        //return repositoryTranscript.findAllByFileIdIn(setTranscriptId).stream()
        return repositoryTranscript.findAllByIdFileIn(setTranscriptId).stream()
                .map(t -> {

                    Optional<EntityFile> f = repositoryFile.findById(t.getIdFile());

                    String parentFolderId = f.isPresent() ? f.get().getParentFolderId() : folderId;

                    return buildDtoTranscript(t, parentFolderId);
                })
                .toList();
    }

    private DtoTranscript buildDtoTranscript(EntityTranscript t, String parentFolderId) {
        List<Optional<EntityTranscriptPage>> listPages = new ArrayList<>();
        for(int n = 1; n <= t.getPageCount(); n++) {
            //todo optimize ? include in all requests ? // remove n ?

            ;
            Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(
                    IdTranscriptPage.createIdTranscriptPage(authService.getConnectedUsername(), t.getIdFile().getFileId(), n));
                        listPages.add(optPage);
        }

        DtoTranscript dtoTranscript = DtoTranscript.fromEntities(t,
                listPages.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList());

        List<DtoTranscriptPage> listP = dtoTranscript.getPages();

        listP.stream()
            .map(page->{
                try {
                    page.setImageUrl(utilsService.imageURL(page.getFileId(), page.getPageNumber()));
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

        dtoTranscript.setPages(listP);
        return dtoTranscript;
    }

    @Override
    public File createTranscriptPdf(String fileId) throws IOException {
        return pdfService.createTranscriptPdf(fileId, Collections.singletonList(getTranscript(fileId)));
    }

    @Override
    public File createTranscriptPdfFromFolder(String folderId) throws IOException {
        return pdfService.createTranscriptPdf(folderId, listTranscriptFromFolderRecurs(folderId));
    }
}
