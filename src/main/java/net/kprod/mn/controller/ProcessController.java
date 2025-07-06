package net.kprod.mn.controller;

import net.kprod.mn.data.dto.DtoProcess;
import net.kprod.mn.service.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.List;

@Controller
public class ProcessController {
    private Logger LOG = LoggerFactory.getLogger(ProcessController.class);

    @Autowired
    private ProcessService processService;

    @GetMapping("/process/cancel/{id}")
    public ResponseEntity<String> processCancel(@PathVariable String id) {
        processService.cancelProcess(id);
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/process/list")
    public ResponseEntity<List<DtoProcess>> viewProcessing(Model model) throws IOException {
        List<DtoProcess> list = processService.listProcess();

        return ResponseEntity.ok().body(list);
    }
}