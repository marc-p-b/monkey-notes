package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private DriveService driveService;

    @Autowired
    private ViewService viewService;

    @Value("${app.drive.folders.in}")
    private String folderIn;

    @GetMapping("/")
    public String home(Model model) {

        Optional<String> optAuthUrl = driveService.requireAuth();

        List<FileNode> listFiles = viewService.listFolders();
        model.addAttribute("fileNodes", listFiles);
        model.addAttribute("authUrl", optAuthUrl.isPresent() ? optAuthUrl.get() : "");
        model.addAttribute("inFolderId", folderIn);
        return "home";
    }

    @GetMapping("/transcript/{fileId}")
    public String getTranscript(Model model, @PathVariable String fileId) throws IOException {
        //return ResponseEntity.ok().body();

        model.addAttribute("dtoTranscript", viewService.getTranscript(fileId));

        return "transcript";
    }

}
