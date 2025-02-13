package net.kprod.dsb.service;

import com.google.api.services.drive.model.File;
import net.kprod.dsb.data.entity.Doc;

import java.io.IOException;
import java.util.List;

public interface DriveChangeManagerService {
    void updateAll();
    void updateFolder(String folderId);
    void watchStop() throws IOException;
    void watch();
    void renewWatch() throws IOException;
    List<String> getWaitList();
    void getChanges(String channelId);
    void flushChanges();
    File processTranscript(String name, String fileId, java.io.File file);
    void refreshFolder(String folderId, String offset, int max_depth, String folder, String currentFolderName, List<Doc> updatedDocs);
}
