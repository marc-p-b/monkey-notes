package net.kprod.dsb.service;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.AsyncProcess;
import net.kprod.dsb.monitoring.AsyncResult;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DriveChangeManagerService {
    void updateAll();
    void updateFolder(String folderId);
    void watchStop() throws IOException;
    void watch(boolean renewOrForce);
    void renewWatch() throws IOException;
    void changeNotified(String channelId);
    void flushChanges();
    Map<String, Object> getStatus();
    String updateAncestorsFolders(String fileId) throws ServiceException;
    void forcePageUpdate(String fileId, int pageNumber);
    Map<String, AsyncProcess> getMapAsyncProcess();
    void cancelProcess(String id);
    void requestForceTranscriptUpdate(String fileId);
    void forceTranscriptUpdate(String fileId);

}
