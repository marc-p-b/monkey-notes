package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class ImageServiceImpl implements ImageService {
    private Logger LOG = LoggerFactory.getLogger(ImageService.class);

    @Override
    public byte[] getImage(String fileId, String imagename) {
        // open image
        LOG.info("Qwen request fileId {} image {}", fileId, imagename);

        try {

            return Files.readAllBytes(Paths.get("/tmp/", fileId, imagename));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

}
