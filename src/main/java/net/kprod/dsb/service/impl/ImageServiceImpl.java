package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ImageService;
import net.kprod.dsb.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageServiceImpl implements ImageService {
    private Logger LOG = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private UtilsService utilsService;

    @Override
    @Deprecated
    public byte[] imageBytes(String fileId, int imageNum) {
        // open image
        LOG.info("Qwen request fileId {} image num {}", fileId, imageNum);

        Path pathImage = utilsService.imagePath(fileId, imageNum);

        try {
            //todo may not be suitable for large files... replace me
            return Files.readAllBytes(pathImage);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void streamImage(File file, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    public void efficientStreamImage(File file, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

}
