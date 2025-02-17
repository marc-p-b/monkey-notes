package net.kprod.dsb.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletRequest;
import net.kprod.dsb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class WebhooksController {
    private Logger LOG = LoggerFactory.getLogger(WebhooksController.class);

    @Autowired
    private DriveService driveService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private MailService mailService;

    @Value("${app.drive.folders.out}")
    String outFolderId;

    @Value("${app.email.recipient}")
    private String emailRecipient;

    @Value("${app.changes.listen.on-startup.enabled}")
    private boolean changesListenEnabled;

    @GetMapping("/grant-callback")
    public ResponseEntity<String> grantCallback(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        driveService.grantCallback(code);
        //todo a callback to driveChMgmtService when init auth ?
        if(changesListenEnabled) {
            driveChMgmtService.watch();
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
            //todo ?
            driveUtilsService.deleteSimilarNameFromTranscripts(transciptFileName, outFolderId);

            File pdfTranscriptFile = pdfService.createTranscriptPdf(fileId, transcript);
            driveChMgmtService.processTranscript(transciptFileName, fileId, transcript, pdfTranscriptFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            String[] recipient = {emailRecipient};
            String emailBody = "New transcript available : " + (transcript.length() > 200 ? transcript.substring(0, 200) : transcript);
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
    public ResponseEntity<String> notifyChange(@RequestHeader(value = "X-Goog-Channel-Id", required = false) String channelId) {
        driveChMgmtService.getChanges(channelId);
        return new ResponseEntity<>("Notification received", HttpStatus.OK);
    }

    @GetMapping(value = "/image/{fileId}/{imageId}",produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImageWithMediaType(@PathVariable String fileId, @PathVariable String imageId) throws IOException {
        return driveChMgmtService.getImage(fileId, imageId);
    }
}