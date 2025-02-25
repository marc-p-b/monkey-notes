package net.kprod.dsb.service;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.File2Process;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringData;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    //CompletableFuture<AsyncResult> asyncProcessFiles(MonitoringData monitoringData, List<File2Process> list);
    String getAncestors(String fileId) throws ServiceException;
}
