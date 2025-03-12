package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.entity.EntityFile;
import net.kprod.dsb.data.entity.EntityTranscript;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.service.PdfService;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ViewServiceImpl implements ViewService {

    @Value("${app.drive.folders.in}")
    private String inboundFolderId;

    @Value("${app.drive.folders.out}")
    private String outFolderId;

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

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
            return doc.getTranscript();
        }
        return "no transcript found for " + fileId;
    }

    //todo Common ?
    private List<String>  recursExploreFolders(EntityFile folder, List<String> folders, String offset) {
        if(folders == null) {
            folders = new ArrayList<>();
        }
        List<EntityFile> l = repositoryFile.findAllByParentFolderIdAndTypeIs(folder.getFileId(), FileType.folder);
        for(EntityFile f : l) {

            List<String> files = listTranscriptFromFolder(f.getFileId()).stream()
                    .map(t -> {

                        return offset + " " + t;

                    })

                    .toList();

            folders.add(offset + f.getName());
            folders.addAll(files);

            recursExploreFolders(f, folders, offset + " ");


        }
        return folders;
    }

    //todo common ?
    private String  recursGetTranscriptsFromFolder(EntityFile folder, StringBuilder transcripts) {
        if(transcripts == null) {
            transcripts = new StringBuilder();;
        }
        List<EntityFile> l = repositoryFile.findAllByParentFolderIdAndTypeIs(folder.getFileId(), FileType.folder);
        for(EntityFile f : l) {

            String ts = getTranscriptsFromFolder(f.getFileId()).stream()
                    .map(t -> {

                        return new StringBuilder()
                                .append("Title ").append(t.getName()).append("\n\n")
                                .append("Date ").append(t.getDocumented_at() != null ? "(d)" + t.getDocumented_at() : "(t)" + t.getTranscripted_at()).append("\n\n")
                                .append(t.getTranscript())
                                .append("\n\n-----\n\n");
                    })
                            .collect(Collectors.joining());
            transcripts.append(ts);
            recursGetTranscriptsFromFolder(f, transcripts);
        }
        return transcripts.toString();
    }

    @Override
    public List<String> listFolders() {

        List<String> folders = null;
        try {

            EntityFile folder = repositoryFile.findById(inboundFolderId).orElseThrow(() -> new ServiceException("inbound folder not found"));

            folders = recursExploreFolders(folder, null, "");

        } catch (ServiceException e) {
            //todo log
        }
        return folders;
    }

    @Value("${app.url.self}")
    private String selfUrl;

    @Override
    public List<String> listTranscriptFromFolder(String folderId) {

        return this.getTranscriptsFromFolder(folderId).stream()
                .map(d -> {
                    String url = selfUrl + "/transcript/" + d.getFileId();
                    return new StringBuilder().append(d.getName()).append(" - ").append(url).toString();
                })
                .toList();
    }


    private List<EntityTranscript> getTranscriptsFromFolder(String folderId) {

        List<EntityTranscript> files = repositoryFile.findAllByParentFolderIdAndTypeIs(folderId, FileType.pdf).stream()
                .map(file -> repositoryTranscript.findById(file.getFileId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return files;
    }

    @Autowired
    private PdfService pdfService;

    @Override
    public File createTranscriptPdfFromFolder(String folderId) throws IOException {

        Optional<EntityFile> f = repositoryFile.findById(folderId);

        if(!f.isPresent()) {
            return pdfService.createTranscriptPdf(folderId, "ERROR");
        }


        String folderTranscripts = recursGetTranscriptsFromFolder(f.get(), null);

        return pdfService.createTranscriptPdf(folderId, folderTranscripts);

    }
}
