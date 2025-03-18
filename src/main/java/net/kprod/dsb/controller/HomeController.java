package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.DtoFile;
import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ViewService viewService;

    @GetMapping("/")
    public String home(Model model) {
        List<FileNode> listFiles = viewService.listFolders();
        model.addAttribute("fileNodes", listFiles);
        return "home";
    }
}
