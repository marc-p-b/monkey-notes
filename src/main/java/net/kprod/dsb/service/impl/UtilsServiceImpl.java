package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UtilsServiceImpl implements UtilsService {

    public static final int BUFFER_FILE_READ = 8192;
    private Logger LOG = LoggerFactory.getLogger(UtilsService.class);

    @Value("${app.paths.transcripts-pdf}")
    private String transcriptPath;

    @Value("${app.paths.download}")
    private String downloadPath;

    @Value("${app.paths.images}")
    private String imagePath;

    @Value("${app.url.self}")
    private String selfUrl;

    public static final String DEFAULT_WORKING_DIR = "/tmp";

    public enum WorkingDir {
        download,
        image,
        transcript,
        tmp;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        createDir(Paths.get(downloadPath));
        createDir(Paths.get(imagePath));
        createDir(Paths.get(transcriptPath));
    }

    private void createDir(Path dir) {
        if(!dir.toFile().exists()) {
            dir.toFile().mkdir();
        }
    }

    @Override
    public Path downloadDir(String fileId) {
        return fileWorkingDir(WorkingDir.download, fileId);
    }

    @Override
    public Path transcriptdDir(String fileId) {
        return fileWorkingDir(WorkingDir.transcript, fileId);
    }

    @Override
    public Path imageDir(String fileId) {
        return fileWorkingDir(WorkingDir.image, fileId);
    }

    @Override
    public Path imagePath(String fileId, int imageNumber) {
        Path path = Paths.get(imageDir(fileId).toString(),fileId + "_" + imageNumber + ".jpg");
        return path;
    }

    @Override
    public URL imageURL(String fileId, int imageNumber) throws MalformedURLException {

        return new URL(selfUrl + "/image/" + fileId + "/" + imageNumber);
    }

    @Override
    public Path fileWorkingDir(WorkingDir dirType, String fileId) {
        Path path = Paths.get(DEFAULT_WORKING_DIR, fileId);;

        switch (dirType) {
            case download -> path =  Paths.get(downloadPath, fileId);
            case image -> path =  Paths.get(imagePath, fileId);
            case transcript -> path =  Paths.get(transcriptPath, fileId);
            default -> Paths.get(DEFAULT_WORKING_DIR, fileId);
        }

        if(path.toFile().exists()) {
            return path;

        } else {
            if(path.toFile().mkdir()) {
                LOG.info("created folder {}", fileId);
                return path;
            } else {
                LOG.error("failed to create directory {}", path);
                return Path.of(DEFAULT_WORKING_DIR);
            }
        }

    }

    @Override
    public void efficientStreamFile(File file, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_FILE_READ];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}
