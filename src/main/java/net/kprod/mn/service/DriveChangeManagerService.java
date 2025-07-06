package net.kprod.mn.service;

import java.io.IOException;
import java.util.Map;

public interface DriveChangeManagerService {
    //void updateAll();

    //void updateFolder(String folderId);

    void watchStop() throws IOException;

    void watch(boolean renewOrForce);

    void renewWatch(String username) throws IOException;

    void changeNotified(String channelId);

    void flushChanges();

    Map<String, Object> getStatus();

    //String updateAncestorsFolders(String fileId) throws ServiceException;

    //void forcePageUpdate(String fileId, int pageNumber);

   // void requestForceTranscriptUpdate(String fileId);

    //void forceTranscriptUpdate(String fileId);
}
