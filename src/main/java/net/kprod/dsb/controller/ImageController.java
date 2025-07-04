package net.kprod.dsb.controller;

import net.kprod.dsb.service.ImageService;
import net.kprod.dsb.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;

@Controller
public class ImageController {
    private Logger LOG = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private UtilsService utilsService;

    @GetMapping(value = "/image/{username}/{fileId}/{imageNum}")
    public ResponseEntity<StreamingResponseBody> getImageWithMediaType(@PathVariable String username, @PathVariable String fileId, @PathVariable int imageNum) throws IOException {
        File file = utilsService.imagePath(username, fileId, imageNum).toFile();

        StreamingResponseBody stream = outputStream -> {
            imageService.efficientStreamImage(username, fileId, imageNum, outputStream);
        };

        //todo adaptative to image type if necessary
        return ResponseEntity.ok()
            .contentLength(file.length())
            .contentType(MediaType.IMAGE_JPEG)
            .body(stream);

    }

    @GetMapping(value = "/imagetemp/{username}/{fileId}/{imageNum}")
    public ResponseEntity<StreamingResponseBody> getImageWithMediaTypeTemp(@PathVariable String username, @PathVariable String fileId, @PathVariable int imageNum) throws IOException {
        File file = utilsService.tempImagePath(username, fileId, imageNum).toFile();

        StreamingResponseBody stream = outputStream -> {
            imageService.efficientStreamImage(username, fileId, imageNum, outputStream, true);
        };

        //todo adaptative to image type if necessary
        return ResponseEntity.ok()
                .contentLength(file.length())
                .contentType(MediaType.IMAGE_JPEG)
                .body(stream);

    }
}