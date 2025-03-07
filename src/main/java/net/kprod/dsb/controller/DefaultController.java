package net.kprod.dsb.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.ViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

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
        return ResponseEntity.ok().body(viewService.getTranscript(fileId));
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

//    @GetMapping("/ancestors/{fileId}")
//    public ResponseEntity<String> ancestors(@PathVariable String fileId) throws ServiceException {
//        String s= driveChMgmtService.getAncestors(fileId);
//        return ResponseEntity.ok().body(s);
//    }


    @GetMapping("/folder/list")
    public ResponseEntity<List<String>> viewFolders() {
        return ResponseEntity.ok().body(viewService.listFolders());
    }

//    @GetMapping("/folder/pdf/{folderId}")
//    public ResponseEntity<MultipartFile> folder2Pdf(@PathVariable String folderId) throws IOException {
//
//
//
//        return ResponseEntity.ok().body(viewService.createTranscriptPdfFromFolder(folderId));
//    }

    @GetMapping("/folder/pdf/{folderId}")
    public ResponseEntity<byte[]> getFile(@PathVariable String folderId) throws IOException {
        // Create Headers for "forcing download"
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, "application/pdf");
        // Headers for giving a custom name to the file and also the file extension, in this example .zip
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("").build().toString());
        // Get the bytes from your service (for example an aws bucket)

        byte[] b = readFileToBytes(viewService.createTranscriptPdfFromFolder(folderId));

        return ResponseEntity.ok().headers(httpHeaders).body(b);
    }

    private static byte[] readFileToBytes(File file) throws IOException {


        byte[] bytes = new byte[(int) file.length()];

        FileInputStream fis = null;
        try {

            fis = new FileInputStream(file);

            //read file into bytes[]
            fis.read(bytes);

        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        return bytes;

    }


    //TODO : remove / avoid unifi reqs
    @PostMapping("/inform")
    public ResponseEntity<String> inform(HttpServletRequest request) {
        //driveChMgmtService.updateAll();


        return ResponseEntity.ok().body("OK");
    }

}