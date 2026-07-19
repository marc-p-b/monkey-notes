package fr.monkeynotes.mn.service.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.ViewOptions;
import fr.monkeynotes.mn.data.dto.DtoFile;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;
import fr.monkeynotes.mn.data.dto.agent.*;
import fr.monkeynotes.mn.data.entity.EntityAgent;
import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.enums.FileType;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.data.repository.RepositoryAgent;
import fr.monkeynotes.mn.data.repository.RepositoryFile;
import fr.monkeynotes.mn.service.AgentService;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.PreferencesService;
import fr.monkeynotes.mn.service.ViewService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {
    public static final String KNOWLEDGE_JSON_FILENAME = "knowledge.json";
    private Logger LOG = LoggerFactory.getLogger(AgentServiceImpl.class);

    @Value("${app.openai.api}")
    private String openAiApiKey;

//    @Value("${app.openai.models.default}")
//    private String defaultModel;

    @Value("${app.openai.models.instructions}")
    private String instructionsDefault;

    @Value("${app.openai.models.available}")
    private String agentAvailableModels;

    @Autowired
    private ViewService viewService;

    @Autowired
    private RepositoryAgent repositoryAgent;

    @Autowired
    private AuthService authService;

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private PreferencesService preferencesService;

    //TODO make this multiuser
    private SseEmitter emitter;
    private Long lastId = 0L;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public DtoAgent getOrCreateAssistant(DtoAgentPrepare dtoAgentPrepare, DtoAssistantOptions options) {
        if(options.isForceNew() == true) {
            return this.newAssistant(dtoAgentPrepare, options);
        } else {
            Optional<EntityAgent> agent = repositoryAgent.findById(dtoAgentPrepare.getUuid());
            if(agent.isPresent()) {
                return DtoAgent.of(agent.get());
            } else {
                return this.newAssistant(dtoAgentPrepare, options);
            }
        }
    }

    @Override
    public DtoAgentPrepare prepareAssistant(String fileIds) {
        String agentInstruction = instructionsDefault;
        try {
            instructionsDefault = preferencesService.getPreference(PreferenceKey.agentInstructions);
        } catch (ServiceException e) {
            LOG.error("failed to retrieve preferences", e);
        }

        if(fileIds.isEmpty()) {
            LOG.error("No fileIds provided");
        }
        //TODO process errors

        Set<String> setFileIds = DtoAgent.createIdFilesSet(fileIds, authService.getUsernameFromContext());

//        Optional<EntityAgent> optAgent = Optional.empty();
//        if(setFileIds.size() == 1) {
//            optAgent = repositoryAgent.findById(setFileIds.stream().findFirst().get());
//        }
//
//        if(optAgent.isPresent()) {
//            EntityAgent agent = optAgent.get();
//
//            DtoAgentPrepare agentPrepare = this.getAssistant(agent.getAssistantId());
//            List<DtoAgentMessage> listMessages = this.getThreadMessages(agent.getThreadId());
//
//            listMessages.add(new DtoAgentMessage()
//                    .setCreatedAt(agentPrepare.getCreatedAt())
//                    .setMessageDir(MessageDir.system)
//                    .setContent(agentPrepare.getInstructions()));
//
//            agentPrepare.setMessages(listMessages.stream()
//                    .sorted(Comparator.comparing(DtoAgentMessage::getCreatedAt))
//                    .toList());
//
//            agentPrepare.setUuid(agent.getUuid());
//            agentPrepare.setFileIds(setFileIds);
//            agentPrepare.setAvailableAIModels(preferencesService.aiModelsFromConfig(agentAvailableModels));
//            return agentPrepare;
//        } else {


            return new DtoAgentPrepare()
                .setUuid(UUID.randomUUID().toString())
                .setFileIds(setFileIds)
                .setAvailableAIModels(preferencesService.aiModelsFromConfig(agentAvailableModels))
                .setSelectedAIModel(preferencesService.getPreferenceOpt(PreferenceKey.selectedAgentModel).orElse(""))
                .setInstructions(agentInstruction);
//        }
    }

    @Override
    public DtoAgent newAssistant(DtoAgentPrepare dtoAgentPrepare, DtoAssistantOptions options) {

        List<IdFile> list = dtoAgentPrepare.getFileIds().stream()
                .map(id -> IdFile.createIdFile(authService.getUsernameFromContext(), id))
                .toList();

        List<EntityFile> files = repositoryFile.findAllById(list);


        JSONObject jsonObject = filesToJson(files);


        String knowledgeFileId = uploadKnowledgeFile(jsonObject.toString());
        String vectorId = createKnowledgeVector(knowledgeFileId);

        //add title
        String name = "knowledge document uuid " + dtoAgentPrepare.getUuid();
        String assistantId = createAssistant(name, options.getInstructions(), options.getModel(), vectorId);
        String threadId = createThread();
        LOG.info("Create assistant id {} threadId {}", assistantId, threadId);

        EntityAgent entityAgent = new EntityAgent()
                .setUsername(authService.getUsernameFromContext())
                .setAssistantId(assistantId)
                .setThreadId(threadId)
                .setUuid(dtoAgentPrepare.getUuid())
                .setCreatedDate(OffsetDateTime.now())
                .setLastUsageDate(OffsetDateTime.now())
                .setFileIds(dtoAgentPrepare.getFileIds().stream().collect(Collectors.joining(",")));

        repositoryAgent.save(entityAgent);
        return DtoAgent.of(entityAgent);
    }

    private JSONObject filesToJson(List<EntityFile> files) {
        return new JSONObject()
            .put("documents", files.stream()
                .map(file -> {
                    DtoFile dtoFile = DtoFile.fromEntity(file);
                    if(dtoFile.getType().equals(FileType.folder)) {
                        return new JSONObject().put("folder", folderToJson(dtoFile));
                    } else {
                        DtoTranscript dtoTranscript = viewService.getTranscript(file.getIdFile().getFileId(), ViewOptions.all());
                        return new JSONObject().put("document", transcriptToJson(dtoTranscript));
                    }
                })
                .toList());
    }

    private JSONObject folderToJson(DtoFile dtoFile) {
        //TODO folder desc
        List<JSONObject> jsonList = viewService.listTranscriptFromFolderRecurs(dtoFile.getFileId())
                .stream()
                .map(t -> transcriptToJson(t))
                .toList();
        return new JSONObject()
                .put("folder_name", dtoFile.getName())
                .put("description", "")
                .put("transcripts", jsonList);
    }


    private JSONObject transcriptToJson(DtoTranscript dtoTranscript) {
        return new JSONObject()
            .put("title", dtoTranscript.getTitle())
            .put("date", dtoTranscript.getDocumented_at())
            .put("pages", dtoTranscript.getPages().stream()
                    .map(p -> new JSONObject()
                            .put("page_number", p.getPageNumber())
                            //TODO apply delta !
                            .put("content", p.getTranscript()))
                .toList());
    }

    private String uploadKnowledgeFile(String knowledge) {
        Resource fileAsResource = new ByteArrayResource(knowledge.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return KNOWLEDGE_JSON_FILENAME;
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(openAiApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileAsResource); // 'file' is the param name expected by the server
        body.add("purpose", "assistants");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        String uploadUrl = "https://api.openai.com/v1/files"; // Replace with actual URL

        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
        DocumentContext context = JsonPath.parse(response.getBody());
        String fileId = context.read("$.id");

        LOG.info("Created file id {}", fileId);
        return fileId;
    }

    private String createKnowledgeVector(String knowledgeFileId) {
        JSONObject jsonObject = new JSONObject()
                .put("name", "knowledge vector store")
                .put("file_ids", Collections.singletonList(knowledgeFileId));

        String vectorId = openAiPostRequest("/v1/vector_stores", jsonObject);
        LOG.info("Create knowledge vector id {}", vectorId);
        return vectorId;
    }

    private String createAssistant(String name, String instructions, String model, String knowledgeVectorId) {
        JSONObject jsonObject = new JSONObject()
                .put("name", name)
                .put("instructions", instructions)
                .put("model", model)
                .put("tools",
                        new JSONArray().put(new JSONObject().put("type", "file_search")))
                .put("tool_resources",
                        new JSONObject().put("file_search",
                                new JSONObject().put("vector_store_ids", new JSONArray().put(knowledgeVectorId))));

        String assistantId = openAiPostRequest("/v1/assistants", jsonObject);
        return assistantId;
    }

    private DtoAgentPrepare getAssistant(String assistantId) {
        String path = "/v1/assistants/" + assistantId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        headers.add("OpenAI-Beta", "assistants=v2");

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path; // Replace with actual URL

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        if(response.getStatusCode() == HttpStatus.OK) {
            DocumentContext context = JsonPath.parse(response.getBody());

            //todo long ?
            long createAtInt = context.read("$.created_at", Long.class);
            OffsetDateTime odt = Instant.ofEpochSecond(createAtInt)
                    .atZone(ZoneId.of("Europe/Paris")).toOffsetDateTime();

            DtoAgentPrepare dtoAgentPrepare = new DtoAgentPrepare()
                    .setExists(true)
                    .setModel(context.read("$.model"))
                    .setInstructions(context.read("$.instructions"))
                    .setCreatedAt(odt);

            return dtoAgentPrepare;
        } else {
            return new DtoAgentPrepare();
        }
    }

    private List<DtoAgentMessage> getThreadMessages(String threadId) {
        String path = "/v1/threads/" + threadId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        headers.add("OpenAI-Beta", "assistants=v2");

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path; // Replace with actual URL

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        List<DtoAgentMessage> listMessages = new ArrayList<>();
        if(response.getStatusCode() == HttpStatus.OK) {

            //todo better json parsing

            List<String> roles = JsonPath.read(response.getBody(), "$.data[*].role");
            List<String> contents = JsonPath.read(response.getBody(), "$.data[*].content[0].text.value");

            List<Integer> createdAt = JsonPath.read(response.getBody(), "$.data[*].created_at");
            for(int i = 0; i < roles.size(); i++) {

                OffsetDateTime odt = Instant.ofEpochSecond(createdAt.get(i))
                        .atZone(ZoneId.of("Europe/Paris")).toOffsetDateTime();

                DtoAgentMessage dtoAgentMessage = new DtoAgentMessage()
                        .setMessageDir(MessageDir.valueOf(roles.get(i)))
                        .setContent(contents.get(i))
                        .setCreatedAt(odt);

                listMessages.add(dtoAgentMessage);

            }

        }


        return listMessages;
    }

    @Override
    public String createThread() {
        String threadId = openAiPostRequest("/v1/threads", new JSONObject());
        LOG.info("Create thread id {}", threadId);
        return threadId;
    }

    @Override
    public void addMessage(String threadId, String content) {
        JSONObject jsonObject = new JSONObject()
                .put("role", "user")
                .put("content", content);

        Optional<EntityAgent> agent= repositoryAgent.findEntityAgentByThreadId(threadId);
        if(agent.isPresent()) {
            agent.get().setLastUsageDate(OffsetDateTime.now());
            repositoryAgent.save(agent.get());
        }


        openAiPostRequest("/v1/threads/" + threadId + "/messages", jsonObject);
        LOG.info("Message added to thread id {}", threadId);
    }

    @Override
    public String createRun(DtoAgent agent) {
        JSONObject jsonObject = new JSONObject()
                .put("assistant_id", agent.getAssistantId());

        String runId = openAiPostRequest("/v1/threads/" + agent.getThreadId() + "/runs", jsonObject);
        LOG.info("Created run id {}", runId);
        return runId;
    }

    public boolean getRunStatus(String threadId, String runId) {
        String path = "/v1/threads/" + threadId + "/runs/" + runId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        headers.add("OpenAI-Beta", "assistants=v2");

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path; // Replace with actual URL

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        LOG.info("Requesting run status for {}", runId);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        DocumentContext context = JsonPath.parse(response.getBody());

        //TODO if failed, stop polling ! also check the last response to give the reason to the user

        String status = context.read("$.status");
        LOG.info("Run status is {}", status);
        return status.equals("completed");
    }

    public String getLastResponse(String threadId) {
        String path = "/v1/threads/"+threadId+"/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        headers.add("OpenAI-Beta", "assistants=v2");

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path; // Replace with actual URL

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        DocumentContext context = JsonPath.parse(response.getBody());
        String lastResponse = context.read("$.data[0].content[0].text.value");
        return lastResponse;
    }

    private String openAiPostRequest(String path, JSONObject jsonObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        headers.add("OpenAI-Beta", "assistants=v2");

        //todo can we avoid toString ? (unless we got a conversion issue
        // Bad Request: "{<EOL>  "error": {<EOL>    "message": "Unknown parameter: 'mapType'.",<EOL>    "type": "invalid_request_error",<EOL>    "param": "mapType",<EOL>    "code": "unknown_parameter"<EOL>  }<EOL>}"
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonObject.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path; // Replace with actual URL

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        DocumentContext context = JsonPath.parse(response.getBody());
        String id = context.read("$.id");
        return id;
    }

    public SseEmitter threadRunPolling(String threadId, String runId) {
        LOG.info("Starting polling thread " + threadId + " for run " + runId);
        this.emitter = new SseEmitter(600000L);

        final Runnable pollTask = () -> {
            try {

                LOG.info("polling");
                boolean completed = this.getRunStatus(threadId, runId);

                if(completed) {
                    LOG.info("completed !");
                    String result = this.getLastResponse(threadId);

                    scheduler.shutdown();
                    scheduler = Executors.newScheduledThreadPool(1);

                    this.emitter.send(SseEmitter.event()
                            .name("message")
                            .id("" + lastId++)
                            .data(result));
                } else {
                    LOG.info("still running");

                    this.emitter.send(SseEmitter.event()
                            .name("message")
                            .id("" + lastId++)
                            .data("waiting"));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        scheduler.scheduleAtFixedRate(pollTask, 0, 5, TimeUnit.SECONDS);
        return emitter;
    }

    public List<DtoAgentPrepare> listThreads() {
        List<EntityAgent> list = repositoryAgent.findAllByUsername(authService.getUsernameFromContext());

        list.stream()
                .map(a -> {
                    return new DtoAgentPrepare()
                            .setThreadName(a.getThreadName())
                            .setUuid(a.getUuid());
                })
    }

    //todo when no polling is active ?
    @Scheduled(fixedRate = 30000)
    public void heartbeat() throws IOException {
        this.emitter.send(SseEmitter.event()
                .name("message")
                .id("" + ++lastId)
                .data("heartbeat"));
    }


}
