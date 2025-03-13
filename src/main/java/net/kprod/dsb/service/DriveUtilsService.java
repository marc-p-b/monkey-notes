package net.kprod.dsb.service;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import net.kprod.dsb.ServiceException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DriveUtilsService {

    Path downloadFileFromDrive(String fileId, String filename, Path targetFolder);
    void delete(String fileId);
    String getFileName(String fileId) throws IOException;
    boolean fileHasSpecifiedParents(String fileId, String parentFileId);
    List<String> getDriveParents(String fileId);
    List<File> listFileByName(String name, String folderId) throws IOException;
    void deleteSimilarNameFromTranscripts(String name, String folderId);
    FileList listDriveFilesPropertiesFromFolder(String folderId) throws ServiceException;
    File getDriveFileDetails(String fileId) throws ServiceException;
    boolean isFolder(File file);
    List<File> getAncestorsUntil(File file, String untilFolderId, int max_depth, List<File> ancestors) throws ServiceException;
    File createFolder(String name, String parentFolderId);
    File upload(String name, String folderId, java.io.File file);
}
