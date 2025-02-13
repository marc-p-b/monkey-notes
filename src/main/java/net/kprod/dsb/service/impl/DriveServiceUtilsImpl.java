package net.kprod.dsb.service.impl;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import net.kprod.dsb.DriveFileTypes;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.DriveUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DriveServiceUtilsImpl implements DriveUtilsService {
    private Logger LOG = LoggerFactory.getLogger(DriveServiceUtilsImpl.class);
//    public static final String GOOGLE_DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
//    public static final String GOOGLE_APP_DOC_MIME_TYPE = "application/vnd.google-apps.document";
//    public static final String GOOGLE_APP_SPREADSHEET_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
//    public static final String GOOGLE_APP_PREZ_MIME_TYPE = "application/vnd.google-apps.presentation";

    @Value("${app.drive.folders.in}")
    String inboundFolderId;

    @Value("${app.drive.folders.out}")
    String outFolderId;

    @Autowired
    DriveService driveService;

    @Override
    public File downloadFileFromDrive(String fileId, Path destPath, Path destFile, String filename) {
        if(destPath.toFile().exists()) {
            LOG.info("folder {} already exists", fileId);
            if(destFile.toFile().exists()) {
                if(destPath.toFile().delete()) {
                    LOG.info("deleted folder {}", fileId);
                } else {
                    LOG.info("failed to delete folder {}", fileId);
                }
            }
        } else {
            if(destPath.toFile().mkdir()) {
                LOG.info("created folder {}", fileId);
            } else {
                LOG.error("failed to create directory {}", destPath.toFile().getAbsolutePath());
            }
        }

        File file2Download = null;
        //Download file from g drive
        try {
            LOG.info("download file id {} from gdrive", fileId);

            File file = driveService.getDrive().files().get(fileId).setFields("id, mimeType, md5Checksum").execute();

            if (file.getMimeType().equals(DriveFileTypes.GOOGLE_APP_DOC_MIME_TYPE) ||
                    file.getMimeType().equals(DriveFileTypes.GOOGLE_APP_SPREADSHEET_MIME_TYPE) ||
                    file.getMimeType().equals(DriveFileTypes.GOOGLE_APP_PREZ_MIME_TYPE)) {
                throw new IOException("Google App document cannot be downloaded");
            }

            try (OutputStream outputStream = new FileOutputStream(destFile.toFile())) {
                driveService.getDrive().files().get(fileId).executeMediaAndDownloadTo(outputStream);
            }
            file2Download = file;
            LOG.info("Downloaded name {} to {}", filename, destPath);
        } catch (IOException e) {
            LOG.error("Failed to download file {}", fileId, e);
        }
        return file2Download;
    }

    @Override
    public void delete(String fileId) {
        LOG.info("delete file {}", fileId);

        try {
            driveService.getDrive().files().delete(fileId).execute();

            LOG.info("deleted file {}", fileId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileName(String fileId) throws IOException {
        File file = driveService.getDrive().files().get(fileId).setFields("name").execute();
        return file.getName();
    }

    public boolean checkInboundFile(String fileId) {
        File file = null;
        try {
            file = driveService.getDrive().files().get(fileId).setFields("parents, mimeType, md5Checksum").execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(file.getMimeType().equals(DriveFileTypes.GOOGLE_DRIVE_FOLDER_MIME_TYPE)) {
            LOG.info("{} is a folder, rejected", fileId);
            return false;
        }

        //level 1
        Optional<String> level1FolderId = checkDriveParents(file.getParents());
        if(level1FolderId.isPresent()) {
            //cache this value
            //LOG.info("parent {} is ok", level1FolderId.get());
            return true;
        } else {
            Optional<String> folderId = recursCheck(file.getParents());
            return folderId.isPresent();
            //LOG.info("parent {} is ok", folderId.orElse("nope !"));
        }

        //return false;
    }

    public Optional<String> recursCheck(List<String> fileIds) {

        List<String> checkedId = new ArrayList<>();

        for(String fileId : fileIds) {
            List<String> lvlParents = getDriveParents(fileId);
            if(lvlParents == null || lvlParents.isEmpty()) {
                //max reached
                return Optional.empty();
            }
            checkedId.addAll(lvlParents);
            Optional<String> lvlFolderId = checkDriveParents(lvlParents);
            if(lvlFolderId.isPresent()) {
                //cache this
                LOG.info("parent {} is ok", lvlFolderId.get());
                return lvlFolderId;
            }
        }
        LOG.info("go recursCheck with ids {}", checkedId);
        return recursCheck(checkedId);
    }

    public List<String> getDriveParents(String fileId) {
        try {
            File dFile = driveService.getDrive().files().get(fileId).setFields("parents").execute();
            return dFile.getParents();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //todo folder param
    public Optional<String> checkDriveParents(List<String> parents) {
       return parents.stream()
                .filter(id -> inboundFolderId.equals(id))
                .findFirst();
    }


    public List<File> listFileByName(String name, String folderId) throws IOException {
        String query = "'" + folderId + "' in parents and name='" + name + "'and trashed = false";
        FileList result = driveService.getDrive().files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();

        return result.getFiles();
    }

    //todo folder id as param
    @Override
    public void deleteSimilarNameFromTranscripts(String name) {
        LOG.info("delete previous files {}", name);
        try {
            List<File> list = listFileByName(name, outFolderId);
            for(File file : list) {
                LOG.info("deleted file {} id {}", file.getName(), file.getId());
                delete(file.getId());
            }
        } catch (IOException e) {
            LOG.error("file does not exists with name {}", name, e);
        }
    }
}
