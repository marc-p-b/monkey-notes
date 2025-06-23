package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.AsyncProcess;
import net.kprod.dsb.data.dto.DtoProcess;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.service.DriveChangeManagerService;
import org.jetbrains.annotations.NotNull;
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

    //@GetMapping("/processing/get")
    @GetMapping("/process/list")
    public ResponseEntity<List<DtoProcess>> viewProcessing(Model model) throws IOException {
        List<DtoProcess> list = driveChangeManagerService.listProcess();

        return ResponseEntity.ok().body(list);
    }


}