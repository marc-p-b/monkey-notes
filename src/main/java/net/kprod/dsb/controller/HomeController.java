package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private DriveService driveService;

    @Autowired
    private ViewService viewService;

    @GetMapping("/")
    public String home(Model model) {

        Optional<String> optAuthUrl = driveService.requireAuth();

        List<FileNode> listFiles = viewService.listFolders();
        model.addAttribute("fileNodes", listFiles);
        model.addAttribute("authUrl", optAuthUrl.isPresent() ? optAuthUrl.get() : "");
        return "home";
    }
}
