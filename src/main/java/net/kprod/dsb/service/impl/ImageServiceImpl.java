package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ImageService;
import net.kprod.dsb.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
public class ImageServiceImpl implements ImageService {
    public static final int IMAGE_READ_BUFFER = 8192;
    private Logger LOG = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private UtilsService utilsService;

    @Override
    public void efficientStreamImage(String username, String fileId, int imageNum, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = new FileInputStream(utilsService.imagePath(username, fileId, imageNum).toFile())) {
            byte[] buffer = new byte[IMAGE_READ_BUFFER];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

}
