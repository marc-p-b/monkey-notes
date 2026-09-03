package fr.monkeynotes.mn.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.monkeynotes.mn.data.dto.DtoExport;
import fr.monkeynotes.mn.data.entity.*;
import fr.monkeynotes.mn.data.enums.LogOperation;
import fr.monkeynotes.mn.data.repository.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {
    public static final String EXPORT_DATABASE_JSON_NAME = "db.json";
    public static final int EXPORT_IMAGE_READ_BUFFER = 1024;
    public static final String DB_JSON = "db.json";

    private Logger LOG = LoggerFactory.getLogger(ExportService.class);

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private RepositoryTranscriptPageDiff repositoryTranscriptPageDiff;

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    @Autowired
    private RepositoryNamedEntityIndex repositoryNamedEntityIndex;

    @Autowired
    private RepositoryConfig repositoryConfig;

    @Autowired
    private AuthService authService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private LogService logService;

    @Autowired
    private RepositoryQuickNote repositoryQuickNote;

    public void export(OutputStream outputStream) throws IOException {

        String username = authService.getUsernameFromContext();
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<EntityTranscriptPage> pages = repositoryTranscriptPage.findByIdTranscriptPage_Username(username);

        DtoExport dtoExport = new DtoExport()
                .setFiles(repositoryFile.findAllByIdFile_Username(username))
                .setTranscripts(repositoryTranscript.findAllByIdFile_Username(username))
                .setPages(pages)

                .setPageDiffs(repositoryTranscriptPageDiff.findAllByIdTranscriptPageDiff_Username(username))
                .setNamedEntities(repositoryNamedEntity.findAllByIdNamedEntity_Username(username))
                .setNamedEntityIndexes(repositoryNamedEntityIndex.findAllByIdNamedEntityIndex_Username(username))

                .setQuickNotes(repositoryQuickNote.findAllByIdQuickNote_Username(username))

                .setPreferences(repositoryConfig.findAllByConfigId_Username(username));

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for(EntityTranscriptPage page : pages) {
                String fileId = page.getIdTranscriptPage().getFileId();
                int pageNumber = page.getIdTranscriptPage().getPageNumber();
                addImage(fileId, pageNumber, zipOut);
            }

            ZipEntry jsonEntry = new ZipEntry(EXPORT_DATABASE_JSON_NAME);
            zipOut.putNextEntry(jsonEntry);

            byte[] jsonBytes = mapper.writeValueAsBytes(dtoExport);
            zipOut.write(jsonBytes);

            zipOut.finish();
        }
        logService.success(LogOperation.exportBackup);
    }

    private void addImage(String fileId, int pageNumber, ZipOutputStream zipOut) throws IOException {
        byte[] buffer = new byte[EXPORT_IMAGE_READ_BUFFER];
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

    public void importUserData(MultipartFile multipartFile) {
        byte[] databaseBytes = null;
        try {
            try (ZipInputStream zis = new ZipInputStream(multipartFile.getInputStream())) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {

                    if(zipEntry.isDirectory() == false && zipEntry.getName().equals(DB_JSON)) {
                        //todo use this instead / import problems
                        //BoundedInputStream bounded = new BoundedInputStream(zis, zipEntry.getSize());

                        ByteArrayOutputStream specialFileBuffer = new ByteArrayOutputStream();
                        zis.transferTo(specialFileBuffer);
                        databaseBytes = specialFileBuffer.toByteArray();

                    }
                    else if(zipEntry.isDirectory() == false) {
                        String basename = FilenameUtils.getBaseName(zipEntry.getName());
                        String fileId = basename.substring(0, basename.lastIndexOf('_'));

                        Path folder = Paths.get(utilsService.getUserImagesPath().toString(), fileId);

                        Path destPath = Paths.get(folder.toString(), zipEntry.getName());
                        LOG.info("Importing " + zipEntry.getName());

                        if (!Files.exists(folder)) {
                            Files.createDirectories(folder);
                        }

                        try (OutputStream os = Files.newOutputStream(destPath)) {
                            zis.transferTo(os);
                        }
                    }
                    zis.closeEntry();
                }
            }
            dbLoad(databaseBytes);
        } catch (IOException e) {
            logService.failure(LogOperation.importBackup);
            LOG.error("Error while importing user data", e);
        }
        logService.success(LogOperation.importBackup);
    }

    private void dbLoad(byte[] databaseBytes) throws IOException {
        LOG.info("Importing " + DB_JSON);
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            DtoExport dtoExport = mapper.readValue(databaseBytes, DtoExport.class);

            String connectedUsername = authService.getUsernameFromContext();

            dtoExport.getFiles()
                    .forEach(f -> {
                        f.setIdFile(IdFile.createIdFile(connectedUsername, f.getIdFile().getFileId()));
                    });
            dtoExport.getTranscripts()
                    .forEach(f -> {
                        f.setIdFile(IdFile.createIdFile(connectedUsername, f.getIdFile().getFileId()));
                    });
            dtoExport.getPages()
                    .forEach(f -> {
                        f.setIdTranscriptPage(IdTranscriptPage.createIdTranscriptPage(
                                connectedUsername,
                                f.getIdTranscriptPage().getFileId(),
                                f.getIdTranscriptPage().getPageNumber()));
                    });
            dtoExport.getPageDiffs()
                    .forEach(f -> {
                        f.setIdTranscriptPageDiff(IdTranscriptPageDiff.createIdTranscriptPageDiff(
                                connectedUsername,
                                f.getIdTranscriptPageDiff().getFileId(),
                                f.getIdTranscriptPageDiff().getPageNumber(),
                                f.getIdTranscriptPageDiff().getVersion()));
                    });
            dtoExport.getNamedEntities()
                    .forEach(f -> {
                        f.setIdNamedEntity(IdNamedEntity.createIdNamedEntity(
                                connectedUsername,
                                f.getIdNamedEntity().getFileId(),
                                f.getIdNamedEntity().getPageNumber()));
                    });
            dtoExport.getNamedEntityIndexes()
                    .forEach(f -> {
                        f.setIdNamedEntityIndex(IdNamedEntityIndex.createIdNamedEntityIndex(
                                connectedUsername,
                                f.getIdNamedEntityIndex().getVerb(),
                                f.getIdNamedEntityIndex().getValue()));
                    });

            dtoExport.getQuickNotes()
                    .forEach(f -> {
                        //the uuid is kept: it is the fileId the note's named entities are keyed on
                        f.setIdQuickNote(IdQuickNote.createIdQuickNote(
                                connectedUsername,
                                f.getIdQuickNote().getUuid()));
                    });

            dtoExport.getPreferences()
                    .forEach(f -> {
                        f.setConfigId(EntityPreferencesId.createConfigId(connectedUsername, f.getConfigId().getKey()));
                    });

            dropUserData(connectedUsername);

            repositoryConfig.saveAll(dtoExport.getPreferences());
            repositoryFile.saveAll(dtoExport.getFiles());
            repositoryTranscript.saveAll(dtoExport.getTranscripts());
            repositoryTranscriptPage.saveAll(dtoExport.getPages());
            repositoryTranscriptPageDiff.saveAll(dtoExport.getPageDiffs());
            repositoryNamedEntity.saveAll(dtoExport.getNamedEntities());
            repositoryNamedEntityIndex.saveAll(dtoExport.getNamedEntityIndexes());
            repositoryQuickNote.saveAll(dtoExport.getQuickNotes());

            LOG.info("loaded all data files {} transcripts {} pages {} pageDiffs {} namedEntity {} namedEntityIndexes {} quicknotes {}",
                    dtoExport.getFiles().size(),
                    dtoExport.getTranscripts().size(),
                    dtoExport.getPages().size(),
                    dtoExport.getPageDiffs().size(),
                    dtoExport.getNamedEntities().size(),
                    dtoExport.getNamedEntityIndexes().size(),
                    dtoExport.getQuickNotes().size());

        } catch (JsonParseException e) {
            logService.failure(LogOperation.importBackup);
            LOG.error("Json Parse Exception", e);
        }
    }

    /**
     * An import replaces the content of the connected account only — a deleteAll() here would take
     * every other account on the instance with it.
     */
    private void dropUserData(String username) {
        repositoryNamedEntityIndex.deleteAllByIdNamedEntityIndex_Username(username);
        repositoryNamedEntity.deleteAllByIdNamedEntity_Username(username);
        repositoryTranscriptPageDiff.deleteAllByIdTranscriptPageDiff_Username(username);
        repositoryTranscriptPage.deleteAllByIdTranscriptPage_Username(username);
        repositoryTranscript.deleteAllByIdFile_Username(username);
        repositoryFile.deleteAllByIdFile_Username(username);
        repositoryQuickNote.deleteAllByIdQuickNote_Username(username);
        repositoryConfig.deleteAllByConfigId_Username(username);

        LOG.info("dropped all data of {}", username);
    }
}
