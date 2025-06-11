package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.AsyncProcess;
import net.kprod.dsb.data.dto.DtoProcess;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.service.DriveChangeManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Controller
public class ProcessController {
    private Logger LOG = LoggerFactory.getLogger(ProcessController.class);

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @Autowired
    private DriveChangeManagerService driveChangeManagerService;

    @GetMapping("/process/cancel/{id}")
    public ResponseEntity<String> processCancel(@PathVariable String id) {
        driveChMgmtService.cancelProcess(id);
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/processing/get")
    public ResponseEntity<List<DtoProcess>> viewProcessing(Model model) throws IOException {
        Map<String, AsyncProcess> mapAsyncProcess = driveChangeManagerService.getMapAsyncProcess();

        List<DtoProcess> list = mapAsyncProcess.entrySet().stream()
                .map(e -> {
                    AsyncProcess asyncProcess = e.getValue();
                    String processName = asyncProcess.getName();
                    CompletableFuture<AsyncResult> future = asyncProcess.getFuture();
                    String status = "unknown";
                    DtoProcess p = new DtoProcess(e.getKey(), processName);

                    Duration d = Duration.between(asyncProcess.getCreatedAt(), OffsetDateTime.now());
                    p.setDescription(asyncProcess.getDescription());
                    String strDuration = new StringBuilder()
                            .append(d.toHoursPart()).append("h ")
                            .append(d.toMinutesPart()).append("m ")
                            .append(d.toSecondsPart()).append("s ").toString();
                    p.setDuration(strDuration);


                    if (future.isDone()) {
                        try {
                            AsyncResult asyncResult = future.get();
                            status = switch (asyncResult.getState()) {
                                case failed -> "failed";
                                case completed -> "completed in " + asyncResult.getRunTime() + "ms";
                                default -> "unknown";
                            };
                        } catch (InterruptedException | ExecutionException e2) {
                            status = "ERROR while getting process status";
                        }
                    } else {
                        status = "running";
                    }
                    p.setStatus(status);
                    return p;
                })
                .toList();

        return ResponseEntity.ok().body(list);
    }
}