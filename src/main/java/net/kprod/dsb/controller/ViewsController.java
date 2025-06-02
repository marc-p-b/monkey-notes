package net.kprod.dsb.controller;

import net.kprod.dsb.data.ViewOptions;
import net.kprod.dsb.data.dto.DtoTranscriptDetails;
import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.data.enums.ViewOptionsCompletionStatus;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.List;

@Controller
public class ViewsController {

    @Autowired
    private ViewService viewService;


    @GetMapping("/")
    public String home(Model model) {
        return "home";
    }

//    @GetMapping("/folder/list")
//    public ResponseEntity<List<FileNode>> viewFolders() {
//        return ResponseEntity.ok().body(viewService.listAllNodes());
//    }

    @GetMapping("/transcript/{fileId}")
    public String viewTranscript(Model model, @PathVariable String fileId) throws IOException {
        model.addAttribute("dtoTranscript", viewService.getTranscript(fileId, ViewOptions.all()));

        return "transcript";
    }

    @GetMapping("/transcript/{fileId}/failed")
    public String viewTranscriptFailedPages(Model model, @PathVariable String fileId) throws IOException {
        model.addAttribute("dtoTranscript", viewService.getTranscript(fileId, new ViewOptions().setCompletionStatus(ViewOptionsCompletionStatus.failed)));

        return "transcript";
    }

//    @GetMapping("/transcript/list")
//    public ResponseEntity<List<FileNode>> viewFolders() {
//        return ResponseEntity.ok().body(viewService.listFolders());
//    }


    @GetMapping("/agent/{fileId}")
    public String viewAgent(Model model, @PathVariable String fileId) throws IOException {
        model.addAttribute("fileId", fileId);
        return "agent";
    }

    @GetMapping("/transcript/recent")
    public ResponseEntity<List<DtoTranscriptDetails>> viewRecentTranscripts() throws IOException {
        return ResponseEntity.ok().body(viewService.listRecentTranscripts(0, 10));
    }

    @GetMapping("/folder/list")
    public ResponseEntity<List<FileNode>> viewRootFolders() throws IOException {
        return ResponseEntity.ok().body(viewService.listRootLevel());
    }

    @GetMapping("/delete/{fileId}")
    public ResponseEntity<String> delete(@PathVariable String fileId) throws IOException {
        viewService.delete(fileId);
        return ResponseEntity.ok("OK");
    }

}
