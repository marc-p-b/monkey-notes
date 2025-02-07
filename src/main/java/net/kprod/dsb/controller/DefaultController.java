package net.kprod.dsb.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletRequest;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.MailService;
import net.kprod.dsb.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
public class DefaultController {
    private Logger LOG = LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private DriveService driveService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private MailService mailService;

    @Value("${app.email.recipient}")
    private String emailRecipient;

    @GetMapping("/grant-callback")
    public ResponseEntity<String> grantCallback(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        driveService.grantCallback(code);
        return ResponseEntity.ok().body("OK");
    }


    @GetMapping("/info")
    public ResponseEntity<List<String>> info() {
        return ResponseEntity.ok().body(driveService.getWaitList());
    }

    @GetMapping("/flush")
    public ResponseEntity<String> flush() {
        driveService.flushChanges();
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
        String transciptFileName = fileName + "-transcript.pdf";
        try {
            //a trick to force android / autosync app on boox to download file
            driveService.deleteSimilarNameFromTranscripts(transciptFileName);

            File pdfTranscriptFile = pdfService.createTranscriptPdf(fileId, transcript);
            driveService.upload(transciptFileName, pdfTranscriptFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            String[] recipient = {emailRecipient};
            String emailBody = "New transcript available : " + (transcript.length() > 40 ? transcript.substring(0, 40) : transcript);
            emailBody += "...";
            mailService.sendSimpleMessage(
                    recipient,
                    "New transcript : " + fileName, emailBody);
        } catch (Exception e) {
            LOG.error("Email failed", e);
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