package net.kprod.dsb.service.impl;

import net.kprod.dsb.data.File2Process;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringData;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.monitoring.SupplyAsync;
import net.kprod.dsb.service.LegacyProcessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class LegacyProcessFileImpl implements LegacyProcessFile {

    @Value("${app.qwen.launcher}")
    private String pathQwenLauncher;

    @Value("${app.pdf2ppm}")
    private String pathPdf2ppm;

    @Value("${app.dry-run:false}")
    private boolean dryRun;

    private final MonitoringService monitoringService;
    private Logger LOG = LoggerFactory.getLogger(LegacyProcessFileImpl.class);

    public LegacyProcessFileImpl(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Async
    @Override
    public CompletableFuture<AsyncResult> asyncProcessFiles(MonitoringData monitoringData, List<File2Process> list) {
        SupplyAsync sa = null;

        try {
            sa = new SupplyAsync(monitoringService, monitoringData, () -> runListAsyncProcess(list));
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        return CompletableFuture.supplyAsync(sa);
    }

    private void runListAsyncProcess(List<File2Process> list) {
        LOG.info("(Async) Processing {} files", list.size());

        //TODO
//        for (File2Process f : list) {
//            processFile(f.getFileId(), f.getWorkingDir(), f.getFilePath());
//        }
    }

    private void processFile(String fileId, Path workingDir, File file) {
        //sudo apt install poppler-utils
        //https://stackoverflow.com/questions/77410607/zorin-os-python-installation-error-with-pyenv-for-no-module-named-ssl
        //pdftoppm doc1.pdf doc -png
        LOG.info("Processing file {} name {}", fileId, file.getName());

        if(dryRun) {
            LOG.warn("Dry run - no processing");
            return;
        }

        String workPath = "\"" + workingDir.toString() + "\"";
        String filePath = "\"" + file.getAbsolutePath() + "\"";
        String name = "\"" + file.getName() + "\"";

        try {
            String[] cmd = {"/bin/sh", "-c", "cd " + workPath + " && " + pathPdf2ppm + " " + filePath + " " + name + " -png"};
            LOG.info("Executing command: {}", String.join(" ", cmd));
            Process res = Runtime.getRuntime().exec(cmd);
            res.waitFor();
            LOG.info("Done converting pdf to images");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        LOG.info("Ls {}", workingDir);
        String argImages = Arrays.stream(workingDir.toFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".png");
            }
        }))
        .map(File::getAbsolutePath)
        .sorted()
        .collect(Collectors.joining(","));

        LOG.info("Ls res {}", argImages);

        argImages = "\"" + argImages + "\"";
        String fileName = "\"" + file.getName().split("\\.")[0] + "\"";

        try {
            String[] cmd = {"/bin/sh", "-c", "/scripts/run-qwen.sh " + fileId + " " + fileName + " " + argImages};

            LOG.info("Executing command: {}", String.join(" ", cmd));
            Process res = Runtime.getRuntime().exec(cmd);
            res.waitFor();
            LOG.info("Done python Qwen request");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
