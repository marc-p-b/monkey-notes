package net.kprod.dsb.service;

import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DriveUtilsService {

    Path downloadFileFromDrive(String fileId, String filename, Path targetFolder);
    void delete(String fileId);
    String getFileName(String fileId) throws IOException;
    boolean fileHasSpecifiedParents(String fileId, String parentFileId);
    //Optional<String> recursCheck(List<String> fileIds);
    List<String> getDriveParents(String fileId);
    //Optional<String> checkDriveParents(List<String> parents, String parentFileId);
    List<File> listFileByName(String name, String folderId) throws IOException;
    void deleteSimilarNameFromTranscripts(String name, String folderId);
}
