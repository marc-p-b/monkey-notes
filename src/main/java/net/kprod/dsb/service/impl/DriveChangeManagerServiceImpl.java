package net.kprod.dsb.service.impl;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.*;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.kprod.dsb.*;
import net.kprod.dsb.data.CompletionResponse;
import net.kprod.dsb.data.entity.Doc;
import net.kprod.dsb.data.repository.DocRepo;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.DriveUtilsService;
import net.kprod.dsb.service.LegacyProcessFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DriveChangeManagerServiceImpl implements DriveChangeManagerService {
    private Logger LOG = LoggerFactory.getLogger(DriveChangeManagerServiceImpl.class);

    @Value("${app.url.self}")
    private String appHost;

    @Value("${app.notify.path}")
    private String notifyPath;

    @Value("${app.drive.folders.in}")
    private String inboundFolderId;

    @Value("${app.drive.folders.out}")
    private String outFolderId;

    @Value("${app.erase-db:false}")
    private boolean eraseDb;

    @Value("${app.qwen.url}")
    private String qwenApiUrl;

    @Value("${app.qwen.key}")
    private String qwenApiKey;

    @Value("${app.qwen.model}")
    private String qwenModel;

    @Value("${app.qwen.prompt}")
    private String qwenPrompt;

    @Value("${app.changes.expiration}")
    private long changesWatchExpiration;

    @Value("${app.changes.flush}")
    private long flushInterval;

    private String lastPageToken = null;
    private String resourceId = null;
    private Channel channel;
    private Channel responseChannel;
    private String currentChannelId;

    private Map<String, ChangedFile> mapScheduled = new HashMap<>();
    private ScheduledFuture<?> futureFlush;
    private boolean watchChanges = false;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private LegacyProcessFile legacyProcessFile;


    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private DocRepo docRepo;

    @Autowired
    private DriveService driveService;

    @Autowired
    private DriveUtilsService driveUtilsService;


    @EventListener(ApplicationReadyEvent.class)
    void startup() {
        LOG.info("Starting up");
        if(eraseDb) {
            LOG.warn(">>> ERASE DB ON STARTUP");
            docRepo.deleteAll();
        }
    }

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


    private CompletionResponse analyzeImage(Path imagePath, String fileId, String imageName) {

        LOG.info("Qwen request analyse image {}", imagePath);

        try {
            JSONObject content1_url = new JSONObject();

            content1_url.put("url", appHost + "/image/" + fileId + "/" + imageName);

            JSONObject content1 = new JSONObject();
            content1.put("type", "image_url");
            content1.put("image_url", content1_url);

            JSONObject content2 = new JSONObject();
            content2.put("type", "text");
            content2.put("text", qwenPrompt);

            JSONObject messages = new JSONObject();
            messages.put("role", "user");
            messages.put("content", Arrays.asList(content1, content2));

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", qwenModel); //qwen2.5-vl-72b-instruct //qwen-vl-max
            requestBody.put("messages", Arrays.asList(messages));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenApiKey);

            //LOG.info("debug json {}", requestBody.toString());

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            long start = System.currentTimeMillis();
            ResponseEntity<String> response = new RestTemplate().exchange(qwenApiUrl, HttpMethod.POST, requestEntity, String.class);
            long took = System.currentTimeMillis() - start;

            String respBody = response.getBody();
            DocumentContext context = JsonPath.parse(respBody);

            String content = context.read("$.choices[0].message.content");
            String model = context.read("$.model");
            int completion_tokens = context.read("$.usage.completion_tokens");
            int prompt_tokens = context.read("$.usage.prompt_tokens");

            Pattern p = Pattern.compile("\\[(.*?)]", Pattern.DOTALL| Pattern.MULTILINE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                content = m.group(1);
            }

            CompletionResponse completionResponse = new CompletionResponse(fileId, took, model, prompt_tokens, completion_tokens, content);
            //LOG.info("Qwen response: {}", content);
            return completionResponse;
        } catch (Exception e) {
            LOG.error("Failed request model", e);
            return null;
        }

    }


    //private List<Path> pdf2Images(String sourcePath, String targetDir) {

    private List<Path> pdf2Images(String fileId, java.io.File sourceFile, Path targetDir) {
        LOG.info("Converting {} to images", sourceFile);
        List<Path> listImages = null;
        try {
            //java.io.File sourceFile = new java.io.File(sourcePath);
            listImages = new ArrayList<>();

            if (sourceFile.exists()) {
                PDDocument document = Loader.loadPDF(sourceFile);
                PDFRenderer pdfRenderer = new PDFRenderer(document);

                int pageCount = document.getNumberOfPages();
                //System.out.println("Total pages to be converted -> " + pageCount);

                //String fileName = fileId;//sourceFile.getName().replace(".pdf", "");
                for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
                    String filename = fileId + "_" + (pageNumber + 1) + ".png";
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, 150, ImageType.GRAY);
                    Path pathPage = Paths.get(targetDir.toString(), filename);
                    java.io.File outputFile = pathPage.toFile();

                    //System.out.println("Image Created -> " + outputFile.getName());
                    ImageIO.write(image, "png", outputFile);
                    listImages.add(pathPage);
                    LOG.info("Page {} converted as {}", pageNumber, filename);
                }

                document.close();
            } else {
                LOG.error("PDF not found: {}", sourceFile);
            }
            return listImages;

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return null;
    }


    public void updateAll() {
        updateFolder(inboundFolderId);
    }

    public void updateFolder(String folderId) {
        File gFolder = null;
        try {
            gFolder = driveService.getDrive().files().get(folderId).setFields("name").execute();
        } catch (IOException e) {
            LOG.error("Error getting folder name {}", folderId, e);
            return;
        }

        //update db
        List<Doc> updatedDocs = new ArrayList<>();
        refreshFolder(folderId, "", 4, "", gFolder.getName(), updatedDocs);

        //download files if needed
        updatedDocs.stream()
                .filter(Doc::isMarkForUpdate)
                .forEach(d-> {
                        Path destPath = Paths.get("/tmp", d.getFileId());
                        Path destFile = Paths.get(destPath.toString(), d.getFileName());
                        driveUtilsService.downloadFileFromDrive(d.getFileId(), destPath, destFile, d.getFileName());
                        d.setLocalFolder(destPath.toString());
                        //mark 4 update usage is not consistent
                        d.setMarkForUpdate(false);
                });
        docRepo.saveAll(updatedDocs);

        LOG.info("Downloaded files");

        List<File2Process> files2Process = updatedDocs.stream()
            .map(d->{
                Path path2File = Paths.get(d.getLocalFolder(), d.getFileName());
                return new File2Process(d.getFileId(), Paths.get(d.getLocalFolder()), path2File.toFile());
            })
            .toList();


        //TODO test
        legacyProcessFile.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files2Process);

        //files2Process.forEach(f->{
        Map<String, List<CompletionResponse>> mapCompleted = files2Process.stream()
            .flatMap(f-> {
                List<Path> listImages =  pdf2Images(f.getFileId(), f.getFile(), f.getWorkingDir());

                LOG.info("PDF fileId {} name {} image list {}", f.getFileId(), f.getFile().getName(), listImages.size());
                return listImages.stream()
                    .map(d->{
                        CompletionResponse completionResponse = analyzeImage(d, f.getFileId(), d.getFileName().toString());
                        LOG.info("FileId {} Image {} transcript {}", f.getFileId(), d.getFileName(), completionResponse.getTranscript());
                        return completionResponse;
                });
            })
            .collect(Collectors.groupingBy(CompletionResponse::getFileId));

        mapCompleted.entrySet().stream()
                .forEach(e -> {
                    String fileId = e.getKey();

                    Optional<Doc> optDoc = docRepo.findById(fileId);
                    if(optDoc.isPresent()) {
                        Doc doc = optDoc.get();

                    long took = e.getValue().stream().map(CompletionResponse::getTranscriptTook).reduce(0l, Long::sum);
                    int tokensPrompt = e.getValue().stream().map(CompletionResponse::getTokensPrompt).reduce(0, Integer::sum);
                    int tokensCompletion = e.getValue().stream().map(CompletionResponse::getTokensCompletion).reduce(0, Integer::sum);
                    List<String> transcripts = e.getValue().stream()
                            .map(CompletionResponse::getTranscript)
                            .collect(Collectors.toList());

                    //move this to image analysis ?
                    int page = 1;
                    StringBuilder sbTranscripts = new StringBuilder();
                    for(String transcript : transcripts) {

                        sbTranscripts
                                .append(page == 1 ? "" : "\n\n")
                                .append("## Page ").append(page++).append("\n\n").append(transcript);
                    }
                        doc.setAiModel(e.getValue().get(0).getAiModel())
                        .setTranscripted_at(OffsetDateTime.now())
                        .setPagerCount(e.getValue().size())
                        .setTokensPrompt(tokensPrompt)
                        .setTokensResponse(tokensCompletion)
                        .setTranscriptTook(took)
                        .setTranscript(sbTranscripts.toString());
                        docRepo.save(doc);
                    }

                });



    }

    public void watchStop() throws IOException {
        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        driveService.getDrive().channels().stop(responseChannel);
        watchChanges = false;
    }

    public void watch() {
        currentChannelId = UUID.randomUUID().toString();

        OffsetDateTime odt = OffsetDateTime.now().plusSeconds(changesWatchExpiration);

        channel = new Channel()
                .setExpiration(odt.toInstant().toEpochMilli())
                .setType("web_hook")
                .setAddress(appHost + notifyPath)
                .setId(currentChannelId);

        try {
            lastPageToken = driveService.getDrive().changes().getStartPageToken().execute().getStartPageToken();
            responseChannel = driveService.getDrive().changes().watch(lastPageToken, channel).execute();
        } catch (IOException e) {
            LOG.error("Failed to create watch channel", e);
        }

        resourceId = responseChannel.getResourceId();

        long now = System.currentTimeMillis();
        long exp = responseChannel.getExpiration();

        LOG.info("watch response: rs id {} channel id {} lastPageToken {} last for {}", responseChannel.getResourceId(), currentChannelId, lastPageToken, (exp - now));

        taskScheduler.schedule(new RefreshWatchTask(ctx), ZonedDateTime.now().plusSeconds(changesWatchExpiration).toInstant());
        watchChanges = true;
    }

    public void renewWatch() throws IOException {
        LOG.info("renew watch");

        LOG.info("stop watch channel id {}", responseChannel.getResourceId());
        driveService.getDrive().channels().stop(responseChannel);

        this.watch();
    }

    @Override
    public Map<String, Object> getStatus() {
        List<String> listScheduled = mapScheduled.entrySet().stream()
                .map(e->{
                    return new StringBuilder().append(e.getKey()).append(" : ").append(e.getValue()).toString();
                })
                .collect(Collectors.toList());

        long delayToFlush = futureFlush != null ? futureFlush.getDelay(TimeUnit.SECONDS) : -1;

        Map<String, Object> info = new HashMap<>();
        info.put("Watch changes", watchChanges ? "enabled" : "disabled");
        info.put("Next flush", delayToFlush + "s");
        info.put("Scheduled count", "" + listScheduled.size());
        info.put("Scheduled", listScheduled);
        return info;
    }

    @Override
    public List<String> listAvailableTranscripts() {
        return docRepo.findAll().stream() //optimize request
                .filter(d -> d.getTranscripted_at() != null)
                .map(d -> {
                    return new StringBuilder()
                            .append(d.getFileId()).append(" - ").append(d.getFileName())
                            .toString();
                })
                .toList();
    }

    @Override
    public String getTranscript(String fileId) {
        Optional<Doc> optDoc = docRepo.findById(fileId);
        if (optDoc.isPresent()) {
            Doc doc = optDoc.get();
            return doc.getTranscript();
        }
        return "no transcript found for " + fileId;
    }

    public void getChanges(String channelId) {
        //not from current channel watch
        if(channelId.equals(currentChannelId) == false) {
            LOG.info("Rejected notified changes channel {}", channelId);
            return;
        }
        LOG.info("Changes notified channel {}", channelId);


        ChangeList changes = null;
        try {
            changes = driveService.getDrive().changes().list(lastPageToken).execute();
            lastPageToken = changes.getNewStartPageToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(!changes.isEmpty()) {
            for (Change change : changes.getChanges()) {
                ChangedFile changedFile = new ChangedFile(change);

                String fileId = change.getFileId();

                if(change.getFile() != null) {
                    LOG.info(" Change fileId {} name {}", fileId, change.getFile().getName());

                    if(driveUtilsService.fileHasSpecifiedParents(fileId, inboundFolderId)) {
                        if(mapScheduled.containsKey(fileId)) {
                            LOG.info("already got a change for file {}", fileId);
                            mapScheduled.get(fileId).getFuture().cancel(true);
                        }
                        LOG.info(" > accept file change {}", fileId);
                        //todo refresh is deactivated inside task
                        futureFlush = taskScheduler.schedule(new FlushTask(ctx), ZonedDateTime.now().plusSeconds(flushInterval).toInstant());
                        changedFile.setFuture(futureFlush);
                        mapScheduled.put(fileId, changedFile);

                    } else {
                        LOG.info("rejected file {}", fileId);
                    }
                } else {
                    LOG.warn("No file with this id found {}", fileId);
                }
            }
        }
    }

    //todo have to fix concurrent flush
    @Override
    public synchronized void flushChanges() {
        long now = System.currentTimeMillis();
        Set<String> setDone = mapScheduled.entrySet().stream()
                //filter changes by time passed since map insertion
                .filter(e -> now - e.getValue().getTimestamp() > (flushInterval - 1))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        //init processing list
        List<File2Process> list2Process = new ArrayList<>();

        setDone.forEach(fileId -> {
            Change change = mapScheduled.get(fileId).getChange();
            String filename = change.getFile().getName();
            LOG.info("Flushing fileid {} name {}", fileId, filename);


            Optional<Doc> optDoc = docRepo.findById(fileId);

            File file;
            try {
                file = driveService.getDrive().files().get(fileId).setFields("id, name, mimeType, md5Checksum").execute();
            } catch (IOException e) {
                //todo
                throw new RuntimeException(e);
            }

            File2Process file2Process = null;
            if(optDoc.isPresent() && file.getMd5Checksum().equals(optDoc.get().getMd5()) == false) {
                //update
                file2Process = new File2Process()
                        .setFileId(fileId)
                        .setMd5(file.getMd5Checksum());
                LOG.info("update file {} {}", fileId, file.getName());

            } else if (optDoc.isPresent() == false) {
                //create
                file2Process = new File2Process()
                        .setFileId(fileId)
                        .setMd5(file.getMd5Checksum());
                LOG.info("create file {} {}", fileId, file.getName());
            } else {
                LOG.info("do nothing file {} {}", fileId, file.getName());

            }

            if(file2Process != null) {
                //create a temp folder folder if needed / remove existing
                Path destPath = Paths.get("/tmp", fileId);
                Path destFile = Paths.get(destPath.toString(), filename);
                File file2Download = driveUtilsService.downloadFileFromDrive(fileId, destPath, destFile, filename);


                //Create file object
                //File2Process f2p = new File2Process(fileId, Paths.get("/tmp", fileId), destFile.toFile());
                //f2p.setMd5(file2Download.getMd5Checksum());
                file2Process
                    .setWorkingDir(destPath)
                    .setFile(destFile.toFile());

                //Add each file to processing list
                list2Process.add(file2Process);
            }
            //remove change
            mapScheduled.remove(fileId);
        });

        //Filter files using md5 / keep only one of each
        List<File2Process> files = list2Process.stream()
            .collect(Collectors.groupingBy(File2Process::getMd5))
            .entrySet().stream().
                map(e -> e.getValue().get(0))
                .toList();

        List<Doc> docs = files.stream()
                .map(f2p -> {
                    return new Doc(f2p.getFileId(), f2p.getFile().getName(), "UNKNOWN", f2p.getMd5()).setMarkForUpdate(true);
                })
                .toList();

        docRepo.saveAll(docs);


        //Request async file list processing
        legacyProcessFile.asyncProcessFiles(monitoringService.getCurrentMonitoringData(), files);

    }

    @Override
    public File processTranscript(String name, String fileId, String transcript, java.io.File file) {
        LOG.info("upload file id {} name {}", fileId, name);

        Optional<Doc> optDoc = docRepo.findById(fileId);
        if(optDoc.isPresent()) {
            Doc doc = optDoc.get();
            doc.setTranscripted_at(OffsetDateTime.now());
            doc.setTranscript(transcript);
            doc.setMarkForUpdate(false);
            docRepo.save(doc);
        } else {
            LOG.error("doc not found {}", fileId);
        }

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(outFolderId));

        FileContent mediaContent = new FileContent("application/pdf", file);
        try {
            File driveFile = driveService.getDrive().files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            System.out.println("File ID: " + driveFile.getId());
            LOG.info("uploaded to drive as fileId {}", driveFile.getId());
            return driveFile;
        } catch (IOException e) {
            LOG.error("error uploading file {}", fileId, e);
        }

        return null;
    }

    @Override
    public void refreshFolder(String folderId, String offset, int max_depth, String folder, String currentFolderName, List<Doc> updatedDocs) {
        String query = "'" + folderId + "' in parents and trashed = false";

        FileList result = null;
        try {
             result = driveService.getDrive().files().list()
                    .setQ(query)
                    .setFields("files(id, mimeType, md5Checksum, name)")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            //System.out.println("No files found.");
        } else {

            for (File file : files) {
                //LOG.info("filename {} id {}", file.getName(), file.getId());

                if(file.getMimeType() != null && file.getMimeType().equals(DriveFileTypes.GOOGLE_DRIVE_FOLDER_MIME_TYPE) && max_depth > 0) {
                    LOG.info("{}{} ({})/",offset, file.getName(), max_depth);
                    refreshFolder(file.getId(), offset + " ", max_depth - 1, folder + "/" + file.getName(), file.getName(), updatedDocs);

                } else {
                    LOG.info(offset + "{} ({})" ,file.getName(), file.getMd5Checksum());

                    Optional<Doc> optDoc = docRepo.findById(file.getId());
                    Doc doc = null;
                    if(optDoc.isPresent() && optDoc.get().getMd5().equals(file.getMd5Checksum()) == false) {
                        //doc has to be updated
                        doc = optDoc.get();
                        doc.setMarkForUpdate(true);

                    } else if (optDoc.isPresent() == false) {
                        //new file : create doc
                        doc = new Doc(file.getId(), file.getName(), folder, file.getMd5Checksum())
                                .setParentFolderId(folderId)
                                .setParentFolderName(currentFolderName)
                                .setMarkForUpdate(true);
                    }
                    docRepo.save(doc);
                    updatedDocs.add(doc);
                }
            }
        }
    }


}
