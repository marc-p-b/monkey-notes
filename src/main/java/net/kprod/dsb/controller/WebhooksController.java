package net.kprod.dsb.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.kprod.dsb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;

@Controller
public class WebhooksController {
    private Logger LOG = LoggerFactory.getLogger(WebhooksController.class);

    @Autowired
    private DriveService driveService;

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private UtilsService utilsService;

    @GetMapping("/grant-callback")
    public ResponseEntity<String> grantCallback(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        driveService.grantCallback(code);

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header("Location", "/").build();
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notifyChange(@RequestHeader(value = "X-Goog-Channel-Id", required = false) String channelId) {
        driveChMgmtService.changeNotified(channelId);
        return new ResponseEntity<>("Notification received", HttpStatus.OK);
    }


    @GetMapping(value = "/image/{fileId}/{imageNum}",produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getImageWithMediaType(@PathVariable String fileId, @PathVariable int imageNum) throws IOException {

        File file = utilsService.imagePath(fileId, imageNum).toFile();

        StreamingResponseBody stream = outputStream -> {
            imageService.efficientStreamImage(fileId, imageNum, outputStream);
        };

        return ResponseEntity.ok()
            .contentLength(file.length())
            .body(stream);

    }
}