package net.kprod.dsb.controller;

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



    @GetMapping("/grant-callback")
    public ResponseEntity<String> grantCallback(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        driveService.grantCallback(code);

        return ResponseEntity.ok().body("OK");
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notifyChange(@RequestHeader(value = "X-Goog-Channel-Id", required = false) String channelId) {
        driveChMgmtService.getChanges(channelId);
        return new ResponseEntity<>("Notification received", HttpStatus.OK);
    }

    //todo secure
    @GetMapping(value = "/image/{fileId}/{imageId}",produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImageWithMediaType(@PathVariable String fileId, @PathVariable String imageId) throws IOException {
        return imageService.getImage(fileId, imageId);
    }
}