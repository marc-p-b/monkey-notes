package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.*;
import net.kprod.dsb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Controller
public class DefaultController {
    private Logger LOG = LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private ExportService exportService;

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Autowired
    private DriveService driveService;

    private SseEmitter emitter;
    private Long lastId = 0L;

    @GetMapping("/authGoogleDrive")
    public ResponseEntity<DtoGoogleDriveConnect> auth() {
        Optional<String> optAuthUrl = driveService.requireAuth();
        if (optAuthUrl.isPresent()) {
            return ResponseEntity.ok(DtoGoogleDriveConnect.disconnected(optAuthUrl.get()));
        } else {
            return ResponseEntity.ok(new DtoGoogleDriveConnect());
        }
    }

    @PostMapping("/import")
    public ResponseEntity<String> importFile(@RequestParam("file") MultipartFile multipartFile) {

        exportService.importUserData(multipartFile);
        return ResponseEntity.ok("OK");
    }

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

    @GetMapping("/transcript/list")
    public ResponseEntity<List<String>> listTranscipt() throws IOException {
        return ResponseEntity.ok().body(viewService.listAvailableTranscripts());
    }

    @GetMapping(value = "/transcript/pdf/{fileId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getTranscriptPdf(@PathVariable String fileId) throws IOException {
        File file = viewService.createTranscriptPdf(fileId);

        StreamingResponseBody stream = outputStream -> {
            utilsService.efficientStreamFile(file, outputStream);
        };

        String docName = driveUtilsService.getFileName(fileId) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + docName)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.length())
                .body(stream);
    }

    @GetMapping("/flush")
    public ResponseEntity<String> flush() {
        driveChMgmtService.flushChanges();
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/update/all")
    public ResponseEntity<String> updateAll() {
        driveChMgmtService.updateAll();
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/update/folder/{folderId}")
    public ResponseEntity<String> updateFolder(@PathVariable String folderId) {
        driveChMgmtService.updateFolder(folderId);
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/update/transcript/{fileId}/{pageNumber}")
    public ResponseEntity<String> formUpdateTranscriptPage(@PathVariable String fileId, @PathVariable int pageNumber) {
        driveChMgmtService.forcePageUpdate(fileId, pageNumber);
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/folder/list")
    public ResponseEntity<List<FileNode>> viewFolders() {
        return ResponseEntity.ok().body(viewService.listFolders());
    }

    @GetMapping(value = "/folder/pdf/{folderId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getFolderPdf(@PathVariable String folderId) throws IOException {
        File file = viewService.createTranscriptPdfFromFolder(folderId);

        StreamingResponseBody stream = outputStream -> {
            utilsService.efficientStreamFile(file, outputStream);
        };

        String docName = driveUtilsService.getFileName(folderId) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + docName)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.length())
                .body(stream);
    }

    @GetMapping("/process/cancel/{id}")
    public ResponseEntity<String> processCancel(@PathVariable String id) {
        driveChMgmtService.cancelProcess(id);
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/transcript/force-update/{fileId}")
    public ResponseEntity<String> forceUpdateTranscript(@PathVariable String fileId) {
        driveChMgmtService.requestForceTranscriptUpdate(fileId);
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> exportUserData() throws IOException {

        StreamingResponseBody stream = outputStream -> {
            exportService.export(outputStream);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=local-files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    @Autowired
    private AgentService agentService;

    public class DtoURL {
        private String url;

        public DtoURL(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    @PostMapping("/agent/ask")
    public ResponseEntity<DtoURL> agentStreamLink(@RequestParam Map<String, String> formData) {
        String question = formData.get("question");
        String fileId = formData.get("fileId");
        boolean forceNewAssistant = Boolean.parseBoolean(formData.get("forceNewAssistant"));
        boolean forceNewThread = Boolean.parseBoolean(formData.get("forceNewThread"));

        DtoAgent dtoAgent = agentService.getOrCreateAssistant(fileId, false);
        agentService.addMessage(dtoAgent.getThreadId(), question);
        String runId = agentService.createRun(dtoAgent);
        String streamLink = "/subscribe/" + dtoAgent.getThreadId() + "/" + runId;

        return ResponseEntity.ok().body(new DtoURL(streamLink));
    }


    @GetMapping("/subscribe/{threadId}/{runId}")
    public SseEmitter subscribe(@PathVariable String threadId, @PathVariable String runId) throws IOException {
        this.emitter = new SseEmitter(600000L);

        startPolling(threadId, runId);
        return this.emitter;
    }

    @Scheduled(fixedRate = 30000)
    public void heartbeat() throws IOException {
        this.emitter.send(SseEmitter.event()
                .name("message")
                .id("" + ++lastId)
                .data("heartbeat"));
    }

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private void startPolling(String threadId, String runId) {
        LOG.info("Starting polling thread " + threadId + " for run " + runId);
        final Runnable pollTask = () -> {
            try {

                LOG.info("polling");
                boolean completed = agentService.getRunStatus(threadId, runId);

                if(completed) {
                    LOG.info("completed !");
                    String result = agentService.getLastResponse(threadId);

                    scheduler.shutdown();
                    scheduler = Executors.newScheduledThreadPool(1);

                    this.emitter.send(SseEmitter.event()
                            .name("message")
                            .id("" + lastId++)
                            .data(result));
                } else {
                    LOG.info("still running");

                    this.emitter.send(SseEmitter.event()
                            .name("message")
                            .id("" + lastId++)
                            .data("waiting"));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        scheduler.scheduleAtFixedRate(pollTask, 0, 5, TimeUnit.SECONDS);
    }

}