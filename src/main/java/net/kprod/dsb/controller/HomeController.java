package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.PreferencesService;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Controller
public class HomeController {

    @Autowired
    private DriveService driveService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private DriveChangeManagerService driveChangeManagerService;
    @Autowired
    private PreferencesService preferencesService;

    @GetMapping("/")
    public String home(Model model) {
        //todo for all user urls
        Optional<String> optAuthUrl = driveService.requireAuth();

        List<FileNode> listFiles = viewService.listFolders();
        model.addAttribute("fileNodes", listFiles);
        model.addAttribute("authUrl", optAuthUrl.isPresent() ? optAuthUrl.get() : "");

        //todo for all user urls
        if(preferencesService.isParametersNotSet()) {
            return "redirect:/preferences";
        }

        return "home";
    }

    @GetMapping("/transcript/{fileId}")
    public String viewTranscript(Model model, @PathVariable String fileId) throws IOException {
        model.addAttribute("dtoTranscript", viewService.getTranscript(fileId));

        return "transcript";
    }

    @GetMapping("/processing")
    public String viewProcessing(Model model) throws IOException {

        Map<String, CompletableFuture<AsyncResult>> mapAsyncProcess = driveChangeManagerService.getMapAsyncProcess();

        //for (Map.Entry<String, CompletableFuture<AsyncResult>> entry : mapAsyncProcess.entrySet()) {

        List<String> list = mapAsyncProcess.entrySet().stream()
                .map(e -> {
                    String processName = e.getKey();
                    CompletableFuture<AsyncResult> future = e.getValue();
                    String status = "unknown";
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
                    return new StringBuilder().append(processName).append(": ").append(status).toString();

                })
                .toList();
        model.addAttribute("processes", list);
        return "processing";
    }

}
