package fr.monkeynotes.mn.service;

import java.io.IOException;
import java.util.Map;

public interface DriveChangeManagerService {
    void watchStop() throws IOException;

    void watch(boolean renewOrForce);

    void renewWatch(String username) throws IOException;

    void changeNotified(String channelId);

    void flushChanges();

    Map<String, Object> getStatus();
}
