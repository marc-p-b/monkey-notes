package net.kprod.dsb.service;

import net.kprod.dsb.ServiceException;

import java.io.IOException;
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
    //CompletableFuture<AsyncResult> asyncProcessFiles(MonitoringData monitoringData, List<File2Process> list);
    String updateAncestorsFolders(String fileId) throws ServiceException;
    void forcePageUpdate(String fileId, int pageNumber);
}
