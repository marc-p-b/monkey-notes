package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoFile;
import net.kprod.dsb.data.dto.DtoTranscript;
import net.kprod.dsb.data.dto.DtoTranscriptPage;
import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.data.entity.EntityFile;
import net.kprod.dsb.data.entity.EntityTranscript;
import net.kprod.dsb.data.entity.EntityTranscriptPage;
import net.kprod.dsb.data.entity.IdTranscriptPage;
import net.kprod.dsb.data.enums.FileType;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.data.repository.RepositoryTranscriptPage;
import net.kprod.dsb.service.PdfService;
import net.kprod.dsb.service.UtilsService;
import net.kprod.dsb.service.ViewService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    RepositoryTranscriptPage repositoryTranscriptPage;

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

//    @Override
//    public String getTranscript(String fileId) {
//        Optional<EntityTranscript> optDoc = repositoryTranscript.findById(fileId);
//        if (optDoc.isPresent()) {
//            EntityTranscript doc = optDoc.get();
//
//            return new StringBuilder()
//                    .append("Title ").append(doc.getName()).append("\n\n")
//                    .append("Date ").append(doc.getDocumented_at() != null ? "(d)" + doc.getDocumented_at() : "(t)" + doc.getTranscripted_at()).append("\n\n")
//                    .append(doc.getTranscript())
//                    .append("\n\n-----\n\n")
//                    .toString();
//
//        }
//        return "no transcript found for " + fileId;
//    }


//    private DtoTranscript htmlDtoFile(String parentFolderId, DtoTranscript dtoTranscript) {



//        return dtoTranscript;
//    }

    @Override
    public DtoTranscript getTranscript2(String fileId) {
        Optional<EntityTranscript> optDoc = repositoryTranscript.findById(fileId);
        Optional<EntityFile> optFile = repositoryFile.findById(fileId);
        if (optDoc.isPresent() && optFile.isPresent()) {

            DtoTranscript dtoTranscript = buildDtoTranscript(optDoc.get(), optFile.get().getParentFolderId());

            //todo common



//                List<URL> list = new ArrayList<>();
//                for(int i = 0; i < dtoTranscript.getPageCount(); i++) {
//
//                    try {
//                        list.add(utilsService.imageURL(fileId, i+1));
//                    } catch (MalformedURLException e) {
//                        LOG.error("Failed to create image URL fileId{} page {}", fileId, i+1);
//                    }
//
//                }
//                dtoTranscript.setPageImages(list);

            // dtoTranscript = htmlDtoFile(optFile.get().getParentFolderId(), dtoTranscript);


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

//todo COMMON and place HERE vvv
            DtoTranscript dtoTranscript = null;
            if (child.getType() == FileType.transcript) {

                Optional<EntityTranscript> optTranscript = repositoryTranscript.findById(child.getFileId());
                if(optTranscript.isPresent()) {


                    //todo is dir.getParentFolderId() OK ?
                    dtoTranscript = buildDtoTranscript(optTranscript.get(), dir.getParentFolderId());

                    //dtoTranscript = DtoTranscript.fromEntity(optTranscript.get());

//                    //todo common TITLE and html
//                    List<URL> list = new ArrayList<>();
//                    for(int i = 0; i < dtoTranscript.getPageCount(); i++) {
//
//                        try {
//                            list.add(utilsService.imageURL(child.getFileId(), i+1));
//                        } catch (MalformedURLException e) {
//                            LOG.error("Failed to create image URL fileId{} page {}", child.getFileId(), i+1);
//                        }
//
//                    }
//                    dtoTranscript.setPageImages(list);
                }
            }

//end try


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

        List<FileNode> folders = null;
        try {

            EntityFile folder = repositoryFile.findById(inboundFolderId).orElseThrow(() -> new ServiceException("inbound folder not found"));

            folders = listFileNodesRecurs(folder);

        } catch (ServiceException e) {
            LOG.error("Error listing folders", e);
            return Collections.emptyList();
        }

        // todo do this in recurs ? (does not work)




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
                .map(t -> {

                    Optional<EntityFile> f = repositoryFile.findById(t.getFileId());

                    String parentFolderId = f.isPresent() ? f.get().getParentFolderId() : folderId;

                    return buildDtoTranscript(t, parentFolderId);
                })
                .toList();
    }

    private DtoTranscript buildDtoTranscript(EntityTranscript t, String parentFolderId) {
        List<Optional<EntityTranscriptPage>> listPages = new ArrayList<>();
        for(int n = 1; n <= t.getPageCount(); n++) {
            //todo optimize ? include in all requests ? // remove n ?

            Optional<EntityTranscriptPage> optPage = repositoryTranscriptPage.findById(new IdTranscriptPage(t.getFileId(), n));
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

        
        Pattern booxBulkExportTitlePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}_\\d{2}_\\d{2}_\\d{2})\\.pdf");
        Matcher m = booxBulkExportTitlePattern.matcher(dtoTranscript.getName());
        //DateTimeFormatter dtfBook = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh_mm_ss");

        if(m.matches()) {
            String strDocDate = m.group(1);
            OffsetDateTime odt = LocalDateTime.parse(strDocDate, DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss")).atZone(ZoneId.systemDefault()).toOffsetDateTime();

            Optional<EntityFile> parentFolder = repositoryFile.findById(parentFolderId);
            dtoTranscript.setTitle(parentFolder.isPresent() ? parentFolder.get().getName() : "unknown");
            dtoTranscript.setDocumented_at(odt);
        }

        return dtoTranscript;
    }

    @Override
    public File createTranscriptPdf(String fileId) throws IOException {
        return pdfService.createTranscriptPdf(fileId, Collections.singletonList(getTranscript2(fileId)));
    }

    @Override
    public File createTranscriptPdfFromFolder(String folderId) throws IOException {

//        String folderTranscripts = listTranscriptFromFolderRecurs(folderId).stream()
//                .map(t -> {
//                    return new StringBuilder()
//                            .append("Title ").append(t.getName()).append("\n\n")
//                            .append("Date ").append(t.getDocumented_at() != null ? "(d)" + t.getDocumented_at() : "(t)" + t.getTranscripted_at()).append("\n\n")
//                            .append(t.getTranscript())
//                            .append("\n\n-----\n\n");
//                })
//                .collect(Collectors.joining());
//
//
//        return pdfService.createTranscriptPdf(folderId, folderTranscripts);



        //return null;

        return pdfService.createTranscriptPdf(folderId, listTranscriptFromFolderRecurs(folderId));
    }
}
