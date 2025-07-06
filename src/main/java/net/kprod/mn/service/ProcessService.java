package net.kprod.mn.service;

import net.kprod.mn.data.dto.DtoProcess;
import net.kprod.mn.data.enums.AsyncProcessName;
import net.kprod.mn.monitoring.AsyncResult;
import net.kprod.mn.monitoring.MonitoringData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ProcessService {
    void registerSyncProcess(AsyncProcessName name, MonitoringData monitoringData, String description, CompletableFuture<AsyncResult> future);
    boolean concurrentProcessFull();
    void cancelProcess(String id);
    List<DtoProcess> listProcess();
}
