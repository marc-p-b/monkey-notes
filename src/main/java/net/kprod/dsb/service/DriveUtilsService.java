package net.kprod.dsb.service;

import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface DriveUtilsService {
    File downloadFileFromDrive(String fileId, Path destPath, Path destFile, String filename);
    void delete(String fileId);
    String getFileName(String fileId) throws IOException;
    boolean checkInboundFile(String fileId);
    Optional<String> recursCheck(List<String> fileIds);
    List<String> getDriveParents(String fileId);
    Optional<String> checkDriveParents(List<String> parents);
    List<File> listFileByName(String name, String folderId) throws IOException;
    void deleteSimilarNameFromTranscripts(String name);
}
