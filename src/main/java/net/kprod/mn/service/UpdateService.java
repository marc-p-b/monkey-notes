package net.kprod.mn.service;

import net.kprod.mn.data.File2Process;

import java.util.List;

public interface UpdateService {
    void runListAsyncProcess(List<File2Process> files2Process);
    void updateAll();
    void updateFolder(String folderId);
    void forcePageUpdate(String fileId, int pageNumber);
    void requestForceTranscriptUpdate(String fileId);
    void forceTranscriptUpdate(String fileId);
}
