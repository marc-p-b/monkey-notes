package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.EntityTranscript;
import fr.monkeynotes.mn.data.entity.EntityUser;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.repository.*;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.UtilsService;
import fr.monkeynotes.mn.service.ViewService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
    private RepositoryFile repositoryFile;

    @Autowired
    private Environment environment;

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private RepositoryTranscriptPageDiff repositoryTranscriptPageDiff;

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    @Autowired
    private RepositoryNamedEntityIndex repositoryNamedEntityIndex;

    @Autowired
    private RepositoryAgent repositoryAgent;

    @EventListener(ApplicationReadyEvent.class)
    public void initUsers() {
        String envInitUser = environment.getProperty("INIT_USERS");

        if((envInitUser!= null && envInitUser.equals("true"))
                || Arrays.stream(environment.getActiveProfiles())
                .filter(p -> p.equals("init_admin"))
                .findFirst().isPresent()) {
            repositoryUser.deleteAll();
            String rndPassword = UUID.randomUUID().toString();

            LOG.info("****** ----------------------- **************");
            LOG.info("****** Initializing admin user **************");
            LOG.info("password " + rndPassword);
            LOG.info("****** ----------------------- **************");

            EntityUser u1 = new EntityUser()
                    .setUsername("admin")
                    .setPassword(new BCryptPasswordEncoder().encode(rndPassword))
                    .setRoles("USER,ADMIN");
            repositoryUser.save(u1);
        }
    }

    @Override
    public void deleteAllData() {
        LOG.info("************** Deleting all data **************");
        repositoryNamedEntity.deleteAll();
        repositoryNamedEntityIndex.deleteAll();
        repositoryTranscriptPageDiff.deleteAll();
        repositoryTranscriptPage.deleteAll();
        repositoryTranscript.deleteAll();
        repositoryFile.deleteAll();
        repositoryAgent.deleteAll();
        LOG.info("**************        Done      ***************");
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
    public Path tempImageDir(String fileId) {
        return fileWorkingDir(getUserImagesPath(), fileId + "_temp");
    }

    @Override
    public Path imagePath(String fileId, int imageNumber) {
        Path path = Paths.get(imageDir(fileId).toString(),fileId + "_" + imageNumber + ".jpg");
        return path;
    }

    @Override
    public Path tempImagePath(String fileId, int imageNumber) {
        Path path = Paths.get(imageDir(fileId + "_temp").toString(),fileId + "_" + imageNumber + ".jpg");
        return path;
    }

    @Override
    public Path imagePath(String username, String fileId, int imageNumber) {
        Path path = Paths.get(userDataBasePath, username, IMAGES, fileId,fileId + "_" + imageNumber + ".jpg");
        return path;
    }

    @Override
    public Path tempImagePath(String username, String fileId, int imageNumber) {
        Path path = Paths.get(userDataBasePath, username, IMAGES, fileId + "_temp",fileId + "_" + imageNumber + ".jpg");
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

    @Override
    public URL tempImageURL(String username, String fileId, int imageNumber) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder()
                .append(selfUrl).append("/imagetemp/")
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

    @Override
    public String getLocalFileName(String fileId) {
        Optional<EntityFile> optionalEntityFile = repositoryFile.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));
        return optionalEntityFile.isPresent() ? optionalEntityFile.get().getName() : fileId;
    }
}
