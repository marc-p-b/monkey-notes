package net.kprod.dsb.service.impl;

import jakarta.transaction.Transactional;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.entity.Doc;
import net.kprod.dsb.data.entity.Folder;
import net.kprod.dsb.data.repository.RepoDoc;
import net.kprod.dsb.data.repository.RepoFolder;
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

    @Autowired
    private RepoDoc repoDoc;

    @Autowired
    private RepoFolder repoFolder;

    @Value("${app.drive.folders.in}")
    private String inboundFolderId;

    @Value("${app.drive.folders.out}")
    private String outFolderId;

    @Override
    public List<String> listAvailableTranscripts() {
        return repoDoc.findAll().stream() //optimize request
                .filter(d -> d.getTranscripted_at() != null)
                .map(d -> {
                    return new StringBuilder()
                            .append(d.getFileId()).append(" - ").append(d.getFileName())
                            .toString();
                })
                .toList();
    }

    @Override
    public String getTranscript(String fileId) {
        Optional<Doc> optDoc = repoDoc.findById(fileId);
        if (optDoc.isPresent()) {
            Doc doc = optDoc.get();
            return doc.getTranscript();
        }
        return "no transcript found for " + fileId;
    }

    private List<String>  recursExploreFolders(Folder folder, List<String> folders, String offset) {
        if(folders == null) {
            folders = new ArrayList<>();
        }
        List<Folder> l = repoFolder.findAllByParentFolderId(folder.getFileId());
        for(Folder f : l) {

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

    private String  recursGetTranscriptsFromFolder(Folder folder, StringBuilder transcripts) {
        if(transcripts == null) {
            transcripts = new StringBuilder();;
        }
        List<Folder> l = repoFolder.findAllByParentFolderId(folder.getFileId());
        for(Folder f : l) {

            String ts = getTranscriptsFromFolder(f.getFileId()).stream()
                    .map(t -> {

                        return new StringBuilder()
                                .append("Title ").append(t.getFileName()).append("\n\n")
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

            Folder folder = repoFolder.findById(inboundFolderId).orElseThrow(() -> new ServiceException("inbound folder not found"));

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

        return repoDoc.findAllByParentFolderId(folderId).stream()
                .map(d -> {
                    String url = selfUrl + "/transcript/" + d.getFileId();
                    return new StringBuilder().append(d.getFileName()).append(" - ").append(url).toString();
                })
                .toList();
    }


    private List<Doc> getTranscriptsFromFolder(String folderId) {

        return repoDoc.findAllByParentFolderId(folderId).stream()

                .toList();
    }

    @Autowired
    private PdfService pdfService;

    @Override
    public File createTranscriptPdfFromFolder(String folderId) throws IOException {

        Optional<Folder> f = repoFolder.findById(folderId);

        if(!f.isPresent()) {
            return pdfService.createTranscriptPdf(folderId, "ERROR");
        }


        String folderTranscripts = recursGetTranscriptsFromFolder(f.get(), null);

        return pdfService.createTranscriptPdf(folderId, folderTranscripts);

    }
}
