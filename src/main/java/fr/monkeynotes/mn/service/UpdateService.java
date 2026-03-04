package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.File2Process;
import fr.monkeynotes.mn.data.MonkeyFileEvent;
import fr.monkeynotes.mn.data.SyncEventResponse;

import java.util.List;

public interface UpdateService {
    void runListAsyncProcess(List<File2Process> files2Process);
    void updateAll();
    void updateFolder(String folderId);
    void forcePageUpdate(String fileId, int pageNumber);
    void requestForceTranscriptUpdate(String fileId);
    void forceTranscriptUpdate(String fileId);
    SyncEventResponse monkeySyncUpdate(MonkeyFileEvent monkeyFileEvent);
    void flushMonkeySync();
    void runListAsyncProcessForUser(List<File2Process> files2Process, String username);
}
