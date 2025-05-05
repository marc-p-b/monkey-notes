package net.kprod.dsb.controller;

import com.google.gson.Gson;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.ViewOptions;
import net.kprod.dsb.data.dto.DtoProcess;
import net.kprod.dsb.data.dto.DtoTranscript;
import net.kprod.dsb.data.dto.DtoTranscriptPage;
import net.kprod.dsb.data.enums.ViewOptionsCompletionStatus;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.DriveUtilsService;
import net.kprod.dsb.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    private static final String OPENAI_API_KEY = "";
    private static final String OPENAI_API_URL = "";

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private DriveChangeManagerService driveChangeManagerService;

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
    public String agent(Model model, @PathVariable String fileId) {
        model.addAttribute("fileId", fileId);
        return "agent";
    }

    @PostMapping("/agent/ask")
    public String agent(Model model, @RequestParam Map<String, String> formData) {
        String knowledge;
        String fileId = formData.get("fileId");

        try {
            com.google.api.services.drive.model.File f = driveUtilsService.getDriveFileDetails(fileId);
            if(driveUtilsService.isFolder(f)) {
                knowledge = viewService.listTranscriptFromFolderRecurs(fileId)
                        .stream()
                        .map(t -> new StringBuilder()
                                .append("\ntitle:").append(t.getTitle())
                                .append(", date:").append(t.getDocumented_at())
                                .append(", content:").append(t.getPages().stream()
                                        .map(DtoTranscriptPage::getTranscript)
                                        .collect(Collectors.joining("\n")))
                                .toString())
                        .collect(Collectors.joining("\n"));
            } else {
                DtoTranscript dtoTranscript = viewService.getTranscript(fileId, ViewOptions.all());
                knowledge = new StringBuilder()
                        .append("title:").append(dtoTranscript.getTitle())
                        .append(", date:").append(dtoTranscript.getDocumented_at())
                        .append(", content:").append(dtoTranscript.getPages().stream()
                                .map(DtoTranscriptPage::getTranscript)
                                .collect(Collectors.joining("\n")))
                        .toString();
            }
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Gson gson = new Gson();
        HttpClient client = HttpClient.newHttpClient();

        String question = formData.get("question");

        Map<String, Object>[] messages = new Map[]{
                Map.of("role", "system", "content", "" +
                        "le document pdf en base de connaissance contient des minutes et des comptes rendus\n" +
                        "ce document a été créé en transformant des notes manuscrites en texte avec un outil OCR ; il y a donc parfois des incohérences notamment orthographiques.\n" +
                        "le but de l'agent est de répondre aux questions en ce basant sur le document en base de connaissance."),
                Map.of("role", "user", "content", "base de connaissance:\n" + knowledge + "\n\nquestion: " + question)
        };

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4.1");
        requestBody.put("messages", messages);

        String json = gson.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        DocumentContext context = JsonPath.parse(response.body());

        String content = context.read("$.choices[0].message.content");

        content = content.replaceAll("\n", "<br/>");

        model.addAttribute("response", content);
        model.addAttribute("fileId", fileId);
        return "agent";
    }


}
