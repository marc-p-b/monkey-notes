package net.kprod.dsb.controller;

import net.kprod.dsb.data.ViewOptions;
import net.kprod.dsb.data.dto.DtoProcess;
import net.kprod.dsb.data.enums.ViewOptionsCompletionStatus;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Controller
public class ViewsController {

    @Autowired
    private ViewService viewService;

    @Autowired
    private DriveChangeManagerService driveChangeManagerService;

    //todo remove this when moved from thymeleaf

    @GetMapping("/")
    public String home(Model model) {
//        //todo for all user urls
//        Optional<String> optAuthUrl = driveService.requireAuth();
//
//        //todo for all user urls
//        if(preferencesService.isParametersNotSet()) {
//            return "redirect:/preferences";
//        }
//
        return "home";
    }

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

    @GetMapping("/processing")
    public String viewProcessing(Model model) throws IOException {

        Map<String, CompletableFuture<AsyncResult>> mapAsyncProcess = driveChangeManagerService.getMapAsyncProcess();

        //for (Map.Entry<String, CompletableFuture<AsyncResult>> entry : mapAsyncProcess.entrySet()) {

        List<DtoProcess> list = mapAsyncProcess.entrySet().stream()
                .map(e -> {
                    String processName = e.getKey();
                    CompletableFuture<AsyncResult> future = e.getValue();
                    String status = "unknown";
                    DtoProcess p = new DtoProcess(processName, processName);
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
                    //return new StringBuilder().append(processName).append(": ").append(status).toString();
                    p.setStatus(status);
                    return p;
                })
                .toList();
        model.addAttribute("processes", list);
        return "processing";
    }


    @GetMapping("/agent/{fileId}")
    public String viewAgent(Model model, @PathVariable String fileId) throws IOException {

        model.addAttribute("fileId", fileId);

        return "agent";

    }

}
