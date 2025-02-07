package net.kprod.dsb.service;

import com.google.api.services.drive.model.File;
import jakarta.servlet.http.HttpServletRequest;
import net.kprod.dsb.ChangedFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DriveService {
    void initAuth();
    void grantCallback(String code);
    void refreshToken();
    List<File> listFileByName(String name, String folderId) throws IOException;

    List<String> getWaitList();
    void watch() throws IOException;
    void renewWatch() throws IOException;
    void watchStop() throws IOException;
    String getFileName(String fileId) throws IOException;
    void downloadFile(String fileId, Path destinationPath) throws IOException;
    void getChanges(String channelId);
    void flushChanges();
    File upload (String name, java.io.File file);
    Optional<String> recursCheck(List<String> fileIds);
    List<String> getDriveParents(String fileId);
    Optional<String> checkDriveParents(List<String> parents);
    boolean checkInboundFile(String fileId) throws IOException;
    void delete(String fileId);
    void deleteSimilarNameFromTranscripts(String name);
}
