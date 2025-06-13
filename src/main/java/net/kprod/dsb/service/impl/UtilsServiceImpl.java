package net.kprod.dsb.service.impl;

import net.kprod.dsb.data.entity.EntityUser;
import net.kprod.dsb.data.repository.RepositoryUser;
import net.kprod.dsb.service.AuthService;
import net.kprod.dsb.service.UtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UtilsServiceImpl implements UtilsService {

    public static final int BUFFER_FILE_READ = 8192;
    public static final String DOWNLOADS = "downloads";
    public static final String IMAGES = "images";
    public static final String TRANSCRIPTS = "transcripts";
    private Logger LOG = LoggerFactory.getLogger(UtilsService.class);

    @Value("${app.paths.user_data}")
    private String userDataBasePath;

    @Value("${app.url.self}")
    private String selfUrl;

    @Autowired
    private RepositoryUser repositoryUser;

    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;


    @EventListener(ApplicationReadyEvent.class)
    public void initUsers() {


        repositoryUser.deleteAll();

        EntityUser u1 = new EntityUser()
                .setUsername("marc")
                .setPassword(new BCryptPasswordEncoder().encode("outsoon4242"))
                .setRoles("USER");

        EntityUser u2 = new EntityUser()
                .setUsername("celine")
                .setPassword(new BCryptPasswordEncoder().encode("outsoon4242"))
                .setRoles("USER");

        EntityUser u3 = new EntityUser()
                .setUsername("marc-test")
                .setPassword(new BCryptPasswordEncoder().encode("outsoon4242"))
                .setRoles("USER");

        repositoryUser.save(u1);
        repositoryUser.save(u2);
        repositoryUser.save(u3);
    }


    @Override
    public Path downloadDir(String fileId) {
        return fileWorkingDir(getUserDownloadsPath(), fileId);
    }

    @Override
    public Path transcriptdDir(String fileId) {
        return fileWorkingDir(getUserTranscriptsPath(), fileId);
    }

    @Override
    public Path imageDir(String fileId) {
        return fileWorkingDir(getUserImagesPath(), fileId);
    }

    @Override
    public Path imagePath(String fileId, int imageNumber) {
        Path path = Paths.get(imageDir(fileId).toString(),fileId + "_" + imageNumber + ".jpg");
        return path;
    }

    @Override
    public Path imagePath(String username, String fileId, int imageNumber) {
        Path path = Paths.get(userDataBasePath, username, IMAGES, fileId,fileId + "_" + imageNumber + ".jpg");
        return path;
    }

    @Override
    public URL imageURL(String username, String fileId, int imageNumber) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder()
                .append(selfUrl).append("/image/")
                .append(username).append("/")
                .append(fileId).append("/")
                .append(imageNumber);

        return new URL(stringBuilder.toString());
    }

    //todo create a read version, avoiding creating false dirs
    private Path fileWorkingDir(Path path, String fileId) {
        path = Paths.get(path.toString(), fileId);
        if(path.toFile().exists()) {
            return path;
        } else {
            if(path.toFile().mkdirs()) {
                LOG.info("created folder {}", fileId);
            } else {
                LOG.error("failed to create directory {}", path);
            }
        }

        //todo throw exc when null
        return path;

    }

    @Override
    public Path getUserDataPath() {
        return Paths.get(userDataBasePath, authService.getUsernameFromContext());
    }

    @Override
    public Path getUserImagesPath() {
        return Paths.get(getUserDataPath().toString(), IMAGES);
    }

    @Override
    public Path getUserDownloadsPath() {
        return Paths.get(getUserDataPath().toString(), DOWNLOADS);
    }

    @Override
    public Path getUserTranscriptsPath() {
        return Paths.get(getUserDataPath().toString(), TRANSCRIPTS);
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
