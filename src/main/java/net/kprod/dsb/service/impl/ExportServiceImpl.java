package net.kprod.dsb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.kprod.dsb.data.dto.DtoExport;
import net.kprod.dsb.data.entity.EntityTranscriptPage;
import net.kprod.dsb.data.repository.RepositoryConfig;
import net.kprod.dsb.data.repository.RepositoryFile;
import net.kprod.dsb.data.repository.RepositoryTranscript;
import net.kprod.dsb.data.repository.RepositoryTranscriptPage;
import net.kprod.dsb.service.AuthService;
import net.kprod.dsb.service.ExportService;
import net.kprod.dsb.service.ImageService;
import net.kprod.dsb.service.UtilsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private RepositoryConfig repositoryConfig;

    @Autowired
    private AuthService authService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private UtilsService utilsService;

    @Override
    public void export(OutputStream outputStream) throws IOException {

        String username = authService.getConnectedUsername();
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<EntityTranscriptPage> pages = repositoryTranscriptPage.findByIdTranscriptPage_Username(username);

        DtoExport dtoExport = new DtoExport()
                .setFiles(repositoryFile.findAllByIdFile_Username(username))
                .setTranscripts(repositoryTranscript.findAllByIdFile_Username(username))
                .setPages(pages)
                .setPreferences(repositoryConfig.findAllByConfigId_Username(username));

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for(EntityTranscriptPage page : pages) {
                String fileId = page.getIdTranscriptPage().getFileId();
                int pageNumber = page.getIdTranscriptPage().getPageNumber();
                addImage(fileId, pageNumber, zipOut);
            }

            ZipEntry jsonEntry = new ZipEntry("db.json");
            zipOut.putNextEntry(jsonEntry);

            byte[] jsonBytes = mapper.writeValueAsBytes(dtoExport);
            zipOut.write(jsonBytes);

            zipOut.finish();
        }
    }

    private void addImage(String fileId, int pageNumber, ZipOutputStream zipOut) throws IOException {
        byte[] buffer = new byte[1024];
        File file = utilsService.imagePath(fileId, pageNumber).toFile();

        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOut.putNextEntry(zipEntry);

            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zipOut.write(buffer, 0, length);
            }

            zipOut.closeEntry();
        }
    }

}
