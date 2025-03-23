package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ImageService;
import net.kprod.dsb.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageServiceImpl implements ImageService {
    private Logger LOG = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private UtilsService utilsService;

    @Override
    public byte[] getImage(String fileId, int imageNum) {
        // open image
        LOG.info("Qwen request fileId {} image num {}", fileId, imageNum);

        Path pathImage = utilsService.imagePath(fileId, imageNum);

        try {

            return Files.readAllBytes(pathImage);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

}
