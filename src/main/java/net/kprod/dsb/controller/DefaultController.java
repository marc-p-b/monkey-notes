package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.DtoGoogleDriveConnect;
import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class DefaultController {
    private Logger LOG = LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private DriveUtilsService driveUtilsService;

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

    @GetMapping("/transcript/force-update/{fileId}")
    public ResponseEntity<String> forceUpdateTranscript(@PathVariable String fileId) {
        driveChMgmtService.requestForceTranscriptUpdate(fileId);
        return ResponseEntity.ok().body("OK");
    }

}