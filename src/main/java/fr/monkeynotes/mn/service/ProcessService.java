package fr.monkeynotes.mn.service;

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
}
