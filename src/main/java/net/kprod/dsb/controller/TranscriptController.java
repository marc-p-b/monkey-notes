package net.kprod.dsb.controller;

import net.kprod.dsb.data.ViewOptions;
import net.kprod.dsb.data.dto.DtoTranscript;
import net.kprod.dsb.data.enums.ViewOptionsCompletionStatus;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

@Controller
public class TranscriptController {

    @Autowired
    private ViewService viewService;

    @GetMapping("/transcript/{fileId}")
    public ResponseEntity<DtoTranscript> getTranscript(@PathVariable String fileId) throws IOException {
//        model.addAttribute("dtoTranscript", viewService.getTranscript(fileId, ViewOptions.all()));
//        return "transcript";

        return ResponseEntity.ok(viewService.getTranscript(fileId, ViewOptions.all()));

    }

    @GetMapping("/transcript/{fileId}/failed")
    public ResponseEntity<DtoTranscript> getTranscriptFailedPages(@PathVariable String fileId) throws IOException {
//        model.addAttribute("dtoTranscript", viewService.getTranscript(fileId, new ViewOptions().setCompletionStatus(ViewOptionsCompletionStatus.failed)));
//        return "transcript";

        return ResponseEntity.ok(viewService.getTranscript(fileId, new ViewOptions().setCompletionStatus(ViewOptionsCompletionStatus.failed)));
    }

}
