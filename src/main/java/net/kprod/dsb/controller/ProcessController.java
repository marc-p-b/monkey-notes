package net.kprod.dsb.controller;

import net.kprod.dsb.service.DriveChangeManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProcessController {
    private Logger LOG = LoggerFactory.getLogger(ProcessController.class);

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @GetMapping("/process/cancel/{id}")
    public ResponseEntity<String> processCancel(@PathVariable String id) {
        driveChMgmtService.cancelProcess(id);
        return ResponseEntity.ok().body("OK");
    }
}