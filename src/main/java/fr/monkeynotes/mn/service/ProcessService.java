package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.AsyncProcessEvent;
import fr.monkeynotes.mn.data.dto.AsyncProcessFileEvent;
import fr.monkeynotes.mn.data.dto.DtoProcess;
import fr.monkeynotes.mn.data.enums.AsyncProcessName;
import fr.monkeynotes.mn.monitoring.AsyncResult;
import fr.monkeynotes.mn.monitoring.MonitoringData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ProcessService {

    void registerSyncProcess(AsyncProcessName name, MonitoringData monitoringData, String description, CompletableFuture<AsyncResult> future);
    boolean concurrentProcessFull();
    void cancelProcess(String id);
    List<DtoProcess> listProcess();

    void updateProcess(String processId, String event);

    void attachFileEvent(String processId, AsyncProcessFileEvent event);

    List<AsyncProcessEvent> getEvents(String processId);
    List<AsyncProcessFileEvent> getFileEvents(String processId);

    List<AsyncProcessFileEvent> getAllFileEvents();
}
