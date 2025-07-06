package net.kprod.mn.controller;

import net.kprod.mn.service.DriveChangeManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Map;

@Controller
public class AdminController {
    private Logger LOG = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @GetMapping("/watch/start")
    public ResponseEntity<String> watchStart() throws IOException {
        driveChMgmtService.watch(true);
        return ResponseEntity.ok().body("watch started");
    }

    @GetMapping("/watch/stop")
    public ResponseEntity<String> watchStop() throws IOException {
        driveChMgmtService.watchStop();
        return ResponseEntity.ok().body("watch stopped");
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok().body(driveChMgmtService.getStatus());
    }

    @GetMapping("/flush")
    public ResponseEntity<String> flush() {
        driveChMgmtService.flushChanges();
        return ResponseEntity.ok().body("OK");
    }

}