package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoFile;
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
    private List<DtoFile>  recursExploreFolders(EntityFile folder, List<DtoFile> folders, int offset) {
        if(folders == null) {
            folders = new ArrayList<>();
        }
        List<EntityFile> listEntityFileFolders = repositoryFile.findAllByParentFolderIdAndTypeIs(folder.getFileId(), FileType.folder);

        for(EntityFile childFolder : listEntityFileFolders) {
            List<DtoFile> listDtoFiles = listTranscriptFromFolder(DtoFile.fromEntity(childFolder)).stream().map(f->f.setOffset(offset + 1)).toList();

            folders.add(DtoFile.fromEntity(childFolder).setOffset(offset));
            folders.addAll(listDtoFiles);

            recursExploreFolders(childFolder, folders, offset + 1);


        }
        return folders;
    }

    //todo common ?
    private String  recursGetTranscriptsFromFolder(EntityFile folder, StringBuilder transcripts) {
//        if(transcripts == null) {
//            transcripts = new StringBuilder();;
//        }
//        List<EntityFile> l = repositoryFile.findAllByParentFolderIdAndTypeIs(folder.getFileId(), FileType.folder);
//        for(EntityFile f : l) {
//
//            String ts = listTranscriptFromFolder2(f.getFileId()).stream()
//                    .map(t -> {
//
//                        return new StringBuilder()
//                                .append("Title ").append(t.getName()).append("\n\n")
//                                .append("Date ").append(t.getDocumented_at() != null ? "(d)" + t.getDocumented_at() : "(t)" + t.getTranscripted_at()).append("\n\n")
//                                .append(t.getTranscript())
//                                .append("\n\n-----\n\n");
//                    })
//                            .collect(Collectors.joining());
//            transcripts.append(ts);
//            recursGetTranscriptsFromFolder(f, transcripts);
//        }
//        return transcripts.toString();
        return null;
    }

    @Override
    public List<DtoFile> listFolders() {

        List<DtoFile> folders = null;
        try {

            EntityFile folder = repositoryFile.findById(inboundFolderId).orElseThrow(() -> new ServiceException("inbound folder not found"));

            folders = recursExploreFolders(folder, null, 0);

        } catch (ServiceException e) {
            //todo log
        }

//        folders.stream()
//                .map(f -> {return DtoFile.fromEntity(f)});


        return folders;
    }

    @Value("${app.url.self}")
    private String selfUrl;

    @Override
    public List<DtoFile> listTranscriptFromFolder(DtoFile folder) {

        int offset = 0;
        return repositoryFile.findAllByParentFolderIdAndTypeIs(folder.getFileId(), FileType.pdf).stream()

                .map(DtoFile::fromEntity)
                .map(f -> f.setOffset(offset))

//                .map(d -> {
//                    String url = selfUrl + "/transcript/" + d.getFileId();
//                    return new StringBuilder().append(d.getName()).append(" - ").append(url).toString();
//                })
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


    private List<DtoFile> listTranscriptFromFolder2(String folderId) {

        return repositoryFile.findAllByParentFolderIdAndTypeIs(folderId, FileType.pdf).stream()
                .map(DtoFile::fromEntity)
                .toList();


    }

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
