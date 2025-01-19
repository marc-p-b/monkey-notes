package net.kprod.dsb.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.File;
import java.io.IOException;

@Controller
public class DefaultController {
    private Logger LOG = LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private DriveService driveService;

    @Autowired
    private PdfService pdfService;

    @GetMapping("/list")
    public ResponseEntity<String> list() {

        try {
            driveService.list();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/watch")
    public ResponseEntity<String> watch() {
        try {
            driveService.watch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/stop")
    public ResponseEntity<String> stop() {
        try {
            driveService.watchStop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body("OK");
    }

    @PostMapping("/transcript")
    public ResponseEntity<String> transcript(@RequestBody String body) {
        LOG.info("Received transcript: {}", body);

        DocumentContext context = JsonPath.parse(body);

        String fileId = context.read("$.fileId");
        String fileName = context.read("$.fileName");
        String transcript = context.read("$.text-content[-1][-1]");
        try {
            File pdfTranscriptFile = pdfService.createTranscriptPdf(fileId, transcript);
            driveService.upload(fileName + ".pdf", pdfTranscriptFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body("OK");
    }


    @PostMapping("/notify")
    public ResponseEntity<String> notifyChange(
            //@RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState,
            //@RequestHeader(value = "X-Goog-Message-Number", required = false) String messageNumber,
            //@RequestHeader(value = "X-Goog-Resource-URI", required = false) String resourceURI,
            @RequestHeader(value = "X-Goog-Channel-Id", required = false) String channelId
            ) {

        //LOG.info("Received notify: channel {}", channelId);
        //LOG.info("Received Drive Notification, message number {}, state {}", messageNumber, resourceState);
        //LOG.info("Message Number: " + messageNumber);
        //LOG.info("resourceURI: " + resourceURI);

        driveService.getChanges(channelId);

        return new ResponseEntity<>("Notification received", HttpStatus.OK);
    }
}