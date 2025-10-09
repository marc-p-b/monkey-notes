package fr.monkeynotes.mn.service.impl;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.DriveFileTypes;
import fr.monkeynotes.mn.service.DriveService;
import fr.monkeynotes.mn.service.DriveUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DriveServiceUtilsImpl implements DriveUtilsService {
    private Logger LOG = LoggerFactory.getLogger(DriveServiceUtilsImpl.class);

    @Autowired
    DriveService driveService;

    @Override
    public Path downloadFileFromDrive(String fileId, String filename, Path targetFolder) {
        Path targetFile = null;
        try {
            LOG.info("download file id {} from gdrive", fileId);

            File file2Download = driveService.getDrive().files().get(fileId).setFields("id, mimeType, md5Checksum, fileExtension").execute();

            if (file2Download.getMimeType().equals(DriveFileTypes.GOOGLE_APP_DOC_MIME_TYPE) ||
                    file2Download.getMimeType().equals(DriveFileTypes.GOOGLE_APP_SPREADSHEET_MIME_TYPE) ||
                    file2Download.getMimeType().equals(DriveFileTypes.GOOGLE_APP_PREZ_MIME_TYPE)) {
                throw new IOException("Google App document cannot be downloaded");
            }
            targetFile = Paths.get(targetFolder.toString(), fileId + "." + file2Download.getFileExtension());

            try (OutputStream outputStream = new FileOutputStream(targetFile.toFile())) {
                driveService.getDrive().files().get(fileId).executeMediaAndDownloadTo(outputStream);
            }
            LOG.info("Downloaded name {} to {}", filename, targetFolder);
        } catch (IOException e) {
            //todo ?
            LOG.error("Failed to download file {}", fileId, e);
        }

        return targetFile;
    }

//    @Override
//    public void delete(String fileId) {
//        LOG.info("delete file {}", fileId);
//
//        try {
//            driveService.getDrive().files().delete(fileId).execute();
//
//            LOG.info("deleted file {}", fileId);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public String getFileName(String fileId) throws IOException {
        File file = driveService.getDrive().files().get(fileId).setFields("name").execute();
        return file.getName();
    }

    @Override
    public FileList listDriveFilesPropertiesFromFolder(String folderId) throws ServiceException {
        String query = "'" + folderId + "' in parents and trashed = false";
        FileList result = null;
        try {
            result = driveService.getDrive().files().list()
                    .setQ(query)
                    .setFields("files(id, mimeType, md5Checksum, name, parents, trashed)")
                    .execute();
        } catch (IOException e) {
            throw new ServiceException("Failed listing drive folder", e);
        }
        return result;
    }

    @Override
    public File getDriveFileDetails(String fileId) throws ServiceException {
        File gFolder;
        try {
            //todo more fields
            gFolder = driveService.getDrive().files().get(fileId).setFields("id, name, mimeType, md5Checksum, parents, trashed").execute();
        } catch (IOException e) {
            throw new ServiceException("Failed getting file properties", e);
        }
        return gFolder;
    }

    @Override
    public boolean fileHasSpecifiedParents(String fileId, String parentFileId) {
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
        Optional<String> level1FolderId = checkDriveParents(file.getParents(), parentFileId);
        if(level1FolderId.isPresent()) {
            //todo cache this value
            return true;
        } else {
            Optional<String> folderId = fileHasSpecifiedParentsRecurs(file.getParents(), parentFileId);
            return folderId.isPresent();
        }
    }

    //todo common with getAncestorsUntil ?
    private Optional<String> fileHasSpecifiedParentsRecurs(List<String> fileIds, String parentFileId) {

        List<String> checkedId = new ArrayList<>();

        for(String fileId : fileIds) {
            List<String> lvlParents = getDriveParents(fileId);
            if(lvlParents == null || lvlParents.isEmpty()) {
                //max reached
                return Optional.empty();
            }
            checkedId.addAll(lvlParents);
            Optional<String> lvlFolderId = checkDriveParents(lvlParents, parentFileId);
            if(lvlFolderId.isPresent()) {
                //todo cache this
                LOG.info("parent {} is ok", lvlFolderId.get());
                return lvlFolderId;
            }
        }
        LOG.info("go recursCheck with ids {}", checkedId);
        return fileHasSpecifiedParentsRecurs(checkedId, parentFileId);
    }

    @Override
    public boolean isFolder(File file) {
       return file.getMimeType().equals(DriveFileTypes.GOOGLE_DRIVE_FOLDER_MIME_TYPE);
    }

    @Override
    public List<File> getAncestorsUntil(File file, String untilFolderId, int max_depth, List<File> ancestors) throws ServiceException {
        if(max_depth == 0) {
            return ancestors;
        }
        if(ancestors == null) {
            ancestors = new ArrayList<>();
        }
        if(file.getParents() != null || file.getParents().isEmpty() == false) {
            if(file.getParents().get(0).equals(untilFolderId)) {
                return ancestors;
            }
            //get first parent file
            File firstParentFile = getDriveFileDetails(file.getParents().get(0));
            if(isFolder(firstParentFile)) {
                ancestors.add(firstParentFile);
                getAncestorsUntil(firstParentFile, untilFolderId, max_depth++, ancestors);
            }
        }
        return ancestors;
    }


    @Override
    public List<String> getDriveParents(String fileId) {
        try {
            File dFile = driveService.getDrive().files().get(fileId).setFields("parents").execute();
            return dFile.getParents();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //todo folder param
    private Optional<String> checkDriveParents(List<String> parents, String parentFileId) {
       return parents.stream()
                .filter(id -> parentFileId.equals(id))
                .findFirst();
    }


    @Override
    public List<File> listFileByName(String name, String folderId) throws IOException {
        String query = "'" + folderId + "' in parents and name='" + name + "'and trashed = false";
        FileList result = driveService.getDrive().files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();

        return result.getFiles();
    }

//    //todo folder id as param
//    @Override
//    public void deleteSimilarNameFromTranscripts(String name, String folderId) {
//        LOG.info("delete previous files {}", name);
//        try {
//            List<File> list = listFileByName(name, folderId);
//            for(File file : list) {
//                LOG.info("deleted file {} id {}", file.getName(), file.getId());
//                delete(file.getId());
//            }
//        } catch (IOException e) {
//            LOG.error("file does not exists with name {}", name, e);
//        }
//    }

    @Override
    public File upload(String name, String folderId, java.io.File file) {
        LOG.info("upload file {}", name);

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(folderId));

        FileContent mediaContent = new FileContent("application/pdf", file);
        try {
            File driveFile = driveService.getDrive().files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            System.out.println("File ID: " + driveFile.getId());
            LOG.info("uploaded to drive as fileId {}", driveFile.getId());
            return driveFile;
        } catch (IOException e) {
            LOG.error("error uploading file {}", name, e);
        }

        return null;
    }

//    @Override
//    public File createFolder(String name, String parentFolderId) {
//        File fileMetadata = new File();
//        fileMetadata.setName(name);
//        fileMetadata.setParents(Collections.singletonList(parentFolderId));
//        fileMetadata.setMimeType(DriveFileTypes.GOOGLE_DRIVE_FOLDER_MIME_TYPE);
//
//        try {
//            File driveFile = driveService.getDrive().files().create(fileMetadata)
//                    .setFields("id, parents")
//                    .execute();
//            return driveFile;
//        } catch (IOException e) {
//            LOG.error("error creating folder {}", name, e);
//        }
//        return null;
//    }
}
