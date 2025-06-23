package net.kprod.dsb.service;

import net.kprod.dsb.data.dto.DtoProcess;
import net.kprod.dsb.data.enums.AsyncProcessName;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ProcessService {
    void registerSyncProcess(AsyncProcessName name, MonitoringData monitoringData, String description, CompletableFuture<AsyncResult> future);
    boolean concurrentProcessFull();
    void cancelProcess(String id);
    List<DtoProcess> listProcess();
}
