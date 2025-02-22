//package net.kprod.dsb.controller;
//
//import net.kprod.dsb.service.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
//import java.io.IOException;
//
//@Controller
//public class LegacyWebhooksController {
//    private Logger LOG = LoggerFactory.getLogger(LegacyWebhooksController.class);
//
//    @Autowired
//    private DriveService driveService;
//
//    @Autowired
//    private DriveUtilsService driveUtilsService;
//
//    @Autowired
//    private DriveChangeManagerService driveChMgmtService;
//
//    @Autowired
//    private PdfService pdfService;
//
//    @Autowired
//    private ImageService imageService;
//
//    @Autowired
//    private MailService mailService;
//
//    @Value("${app.drive.folders.out}")
//    String outFolderId;
//
//    @Value("${app.email.recipient}")
//    private String emailRecipient;
//
//    @Value("${app.changes.listen.on-startup.enabled}")
//    private boolean changesListenEnabled;
//
//
//
//    @PostMapping("/transcript")
//    public ResponseEntity<String> transcript(@RequestBody String body) {
//        LOG.info("Received transcript: {}", body);
//
//        DocumentContext context = JsonPath.parse(body);
//
//        String fileId = context.read("$.fileId");
//        String fileName = context.read("$.fileName");
//        String transcript = context.read("$.text-content[-1][-1]");
//        String transciptFileName = fileName + "-transcript.pdf";
//        try {
//            //a trick to force android / autosync app on boox to download file
//            //todo ?
//            driveUtilsService.deleteSimilarNameFromTranscripts(transciptFileName, outFolderId);
//
//            File pdfTranscriptFile = pdfService.createTranscriptPdf(fileId, transcript);
//            //legacyProcessFile.processTranscript(transciptFileName, fileId, transcript, pdfTranscriptFile);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        try {
//            String[] recipient = {emailRecipient};
//            String emailBody = "New transcript available : " + (transcript.length() > 200 ? transcript.substring(0, 200) : transcript);
//            emailBody += "...";
//            mailService.sendSimpleMessage(
//                    recipient,
//                    "New transcript : " + fileName, emailBody);
//        } catch (Exception e) {
//            LOG.error("Email failed", e);
//        }
//        return ResponseEntity.ok().body("OK");
//    }
//
//}