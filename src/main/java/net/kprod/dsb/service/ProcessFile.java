package net.kprod.dsb.service;

import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringData;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface ProcessFile {
    //void asyncProcessFile(String fileId, Path workingDir, File file);
    CompletableFuture<AsyncResult> asyncProcessFile(MonitoringData monitoringData, String fileId, Path workingDir, File file);
}
