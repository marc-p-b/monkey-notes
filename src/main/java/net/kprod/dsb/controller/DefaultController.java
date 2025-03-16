package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.FileNode;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.ViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class DefaultController {
    private Logger LOG = LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @Autowired
    private ViewService viewService;

    @GetMapping("/watch/start")
    public ResponseEntity<String> watchStart() throws IOException {
        driveChMgmtService.watch();
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

    @GetMapping("/transcript/{fileId}")
    public ResponseEntity<String> getTranscript(@PathVariable String fileId) throws IOException {
        return ResponseEntity.ok().body(viewService.getTranscript(fileId).replaceAll("\n", "<br/>"));
    }

    @GetMapping("/transcript/pdf/{fileId}")
    public ResponseEntity<byte[]> getTranscriptPdf(@PathVariable String fileId) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, "application/pdf");
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("").build().toString());

        byte[] b = readFileToBytes(viewService.createTranscriptPdf(fileId));
        return ResponseEntity.ok().headers(httpHeaders).body(b);
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

    @GetMapping("/folder/list")
    public ResponseEntity<List<FileNode>> viewFolders() {
        return ResponseEntity.ok().body(viewService.listFolders());
    }

    @GetMapping("/folder/pdf/{folderId}")
    public ResponseEntity<byte[]> getFolderPdf(@PathVariable String folderId) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, "application/pdf");
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("").build().toString());

        byte[] b = readFileToBytes(viewService.createTranscriptPdfFromFolder(folderId));
        return ResponseEntity.ok().headers(httpHeaders).body(b);
    }

    private static byte[] readFileToBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(bytes);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return bytes;
    }
}