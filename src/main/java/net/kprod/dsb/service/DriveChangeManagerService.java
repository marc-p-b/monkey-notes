package net.kprod.dsb.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DriveChangeManagerService {
    void updateAll();
    void updateFolder(String folderId);
    void watchStop() throws IOException;
    void watch();
    void renewWatch() throws IOException;
    void getChanges(String channelId);
    void flushChanges();
    Map<String, Object> getStatus();
    List<String> listAvailableTranscripts();
    String getTranscript(String fileId);
}
