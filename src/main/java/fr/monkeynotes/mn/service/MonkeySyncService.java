package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.File2Process;
import fr.monkeynotes.mn.data.MonkeyFileEvent;
import fr.monkeynotes.mn.data.SyncEventResponse;

import java.util.List;

public interface MonkeySyncService {
    SyncEventResponse monkeySyncUpdate2(MonkeyFileEvent monkeyFileEvent, byte[] fileContent);
    SyncEventResponse monkeySyncUpdate(MonkeyFileEvent monkeyFileEvent);
    void flushMonkeySync();
    void runListAsyncProcessForUser(List<File2Process> files2Process, String username);
    String createMonkeySyncId(String input);
}
