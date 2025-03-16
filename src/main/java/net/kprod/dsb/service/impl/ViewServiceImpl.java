package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoFile;
import net.kprod.dsb.data.dto.DtoTranscript;
import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.data.entity.EntityFile;
import net.kprod.dsb.data.entity.EntityTranscript;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.service.PdfService;
import net.kprod.dsb.service.ViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ViewServiceImpl implements ViewService {

    Logger LOG = LoggerFactory.getLogger(ViewServiceImpl.class);

    @Value("${app.drive.folders.in}")
    private String inboundFolderId;

    @Value("${app.drive.folders.out}")
    private String outFolderId;

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private PdfService pdfService;

    @Override
    public List<String> listAvailableTranscripts() {
        return repositoryTranscript.findAll().stream() //optimize request
                .filter(d -> d.getTranscripted_at() != null)
                .map(d -> {
                    return new StringBuilder()
                            .append(d.getFileId()).append(" - ").append(d.getName())
                            .toString();
                })
                .toList();
    }

    @Override
    public String getTranscript(String fileId) {
        Optional<EntityTranscript> optDoc = repositoryTranscript.findById(fileId);
        if (optDoc.isPresent()) {
            EntityTranscript doc = optDoc.get();

            return new StringBuilder()
                    .append("Title ").append(doc.getName()).append("\n\n")
                    .append("Date ").append(doc.getDocumented_at() != null ? "(d)" + doc.getDocumented_at() : "(t)" + doc.getTranscripted_at()).append("\n\n")
                    .append(doc.getTranscript())
                    .append("\n\n-----\n\n")
                    .toString();

        }
        return "no transcript found for " + fileId;
    }

    private List<FileNode> listFileNodesRecurs(EntityFile dir) {
        DtoFile directory = DtoFile.fromEntity(dir);
        List<FileNode> fileNodes = new ArrayList<>();
        List<EntityFile> childen = repositoryFile.findAllByParentFolderId(directory.getFileId());
        for (EntityFile child : childen) {
            FileNode node = new FileNode(DtoFile.fromEntity(child));
            if(child.getType() == FileType.folder) {
                    node.setChildren(listFileNodesRecurs(child));
            }
            fileNodes.add(node);
        }
        return fileNodes;
    }

    private List<DtoFile> listAllFilesRecurs(EntityFile dir) {
        DtoFile directory = DtoFile.fromEntity(dir);
        List<DtoFile> files = new ArrayList<>();
        List<EntityFile> childen = repositoryFile.findAllByParentFolderId(directory.getFileId());
        for (EntityFile child : childen) {
            if (child.getType() == FileType.pdf) {
                files.add(DtoFile.fromEntity(child));
            } if(child.getType() == FileType.folder) {
                files.addAll(listAllFilesRecurs(child));
            }
        }
        return files;
    }

    @Override
    public List<FileNode> listFolders() {

        List<FileNode> folders = null;
        try {

            EntityFile folder = repositoryFile.findById(inboundFolderId).orElseThrow(() -> new ServiceException("inbound folder not found"));

            folders = listFileNodesRecurs(folder);

        } catch (ServiceException e) {
            LOG.error("Error listing folders", e);
            return Collections.emptyList();
        }


        return folders;
    }

    private List<DtoTranscript> listTranscriptFromFolderRecurs (String folderId) {
        Optional<EntityFile> optFolder = repositoryFile.findById(folderId);

        if(!optFolder.isPresent()) {
            return Collections.emptyList();
        }

        Set<String> setTranscriptId = listAllFilesRecurs(optFolder.get()).stream()
                .map(DtoFile::getFileId)
                .collect(Collectors.toSet());

        return repositoryTranscript.findAllByFileIdIn(setTranscriptId).stream()
                .map(DtoTranscript::fromEntity)
                .toList();
    }

    @Override
    public File createTranscriptPdf(String fileId) throws IOException {

        return pdfService.createTranscriptPdf(fileId, getTranscript(fileId));

    }

    @Override
    public File createTranscriptPdfFromFolder(String folderId) throws IOException {

        String folderTranscripts = listTranscriptFromFolderRecurs(folderId).stream()
                .map(t -> {
                    return new StringBuilder()
                            .append("Title ").append(t.getName()).append("\n\n")
                            .append("Date ").append(t.getDocumented_at() != null ? "(d)" + t.getDocumented_at() : "(t)" + t.getTranscripted_at()).append("\n\n")
                            .append(t.getTranscript())
                            .append("\n\n-----\n\n");
                })
                .collect(Collectors.joining());


        return pdfService.createTranscriptPdf(folderId, folderTranscripts);

    }
}
