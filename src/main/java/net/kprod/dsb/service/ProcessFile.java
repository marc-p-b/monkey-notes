package net.kprod.dsb.service;

import net.kprod.dsb.File2Process;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ProcessFile {
    CompletableFuture<AsyncResult> asyncProcessFiles(MonitoringData monitoringData, List<File2Process> list);
}
