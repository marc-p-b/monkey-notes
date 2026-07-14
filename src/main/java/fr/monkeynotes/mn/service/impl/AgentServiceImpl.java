package fr.monkeynotes.mn.service.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.ViewOptions;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;
import fr.monkeynotes.mn.data.dto.agent.*;
import fr.monkeynotes.mn.data.entity.EntityAgent;
import fr.monkeynotes.mn.data.entity.EntityAgentMessage;
import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.IdAgentMessage;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.enums.FileType;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.data.repository.RepositoryAgent;
import fr.monkeynotes.mn.data.repository.RepositoryAgentMessage;
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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AgentServiceImpl implements AgentService {
    public static final String KNOWLEDGE_JSON_FILENAME = "knowledge.json";
    private Logger LOG = LoggerFactory.getLogger(AgentServiceImpl.class);

    @Value("${app.openai.api}")
    private String openAiApiKey;

    @Value("${app.openai.models.instructions}")
    private String instructionsDefault;

    @Value("${app.openai.models.available}")
    private String agentAvailableModels;

    @Autowired
    private ViewService viewService;

    @Autowired
    private RepositoryAgent repositoryAgent;

    @Autowired
    private RepositoryAgentMessage repositoryAgentMessage;

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
    public EntityAgent getOrCreateAgent(String fileId, DtoAssistantOptions options) {
        if (options.isForceNew()) {
            return this.newAgent(fileId, options);
        } else {
            Optional<EntityAgent> agent = repositoryAgent.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));
            return agent.orElseGet(() -> this.newAgent(fileId, options));
        }
    }

    @Override
    public DtoAgentPrepare prepareAssistant(String fileId) {
        String username = authService.getUsernameFromContext();
        Optional<EntityAgent> optAgent = repositoryAgent.findById(IdFile.createIdFile(username, fileId));

        if (optAgent.isPresent()) {
            EntityAgent agent = optAgent.get();

            List<DtoAgentMessage> messages = repositoryAgentMessage
                    .findByIdAgentMessage_UsernameAndIdAgentMessage_FileIdOrderByIdAgentMessage_Sequence(username, fileId)
                    .stream()
                    .map(m -> new DtoAgentMessage()
                            .setMessageDir(m.getMessageDir())
                            .setContent(m.getContent())
                            .setCreatedAt(m.getCreatedAt()))
                    .toList();

            return new DtoAgentPrepare()
                    .setFileId(fileId)
                    .setExists(true)
                    .setModel(agent.getModel())
                    .setInstructions(agent.getInstructions())
                    .setCreatedAt(agent.getCreatedAt())
                    .setMessages(messages)
                    .setAvailableAIModels(preferencesService.aiModelsFromConfig(agentAvailableModels));
        } else {
            String agentInstruction = instructionsDefault;
            try {
                agentInstruction = preferencesService.getPreference(PreferenceKey.agentInstructions);
            } catch (ServiceException e) {
                LOG.error("failed to retrieve preferences", e);
            }

            return new DtoAgentPrepare()
                    .setFileId(fileId)
                    .setAvailableAIModels(preferencesService.aiModelsFromConfig(agentAvailableModels))
                    .setSelectedAIModel(preferencesService.getPreferenceOpt(PreferenceKey.selectedAgentModel).orElse(""))
                    .setInstructions(agentInstruction);
        }
    }

    @Override
    public EntityAgent newAgent(String fileId, DtoAssistantOptions options) {
        LOG.info("New agent based on fileId {}", fileId);

        //TODO create a service to do this kind of operations
        Optional<EntityFile> f = repositoryFile.findById(IdFile.createIdFile(authService.getUsernameFromContext(), fileId));

        if (f.isEmpty()) {
            //TODO error
            return null;
        }

        JSONObject jsonObject;
        if (f.get().getType().equals(FileType.folder)) {
            List<JSONObject> jsonList = viewService.listTranscriptFromFolderRecurs(fileId)
                    .stream()
                    .map(t -> new JSONObject()
                            .put("title", t.getTitle())
                            .put("date", t.getDocumented_at())
                            .put("pages", t.getPages().stream()
                                    .map(DtoTranscriptPage::getTranscript)
                                    .toList()))
                    .toList();
            jsonObject = new JSONObject()
                    .put("documents", jsonList);
        } else {
            DtoTranscript t = viewService.getTranscript(fileId, ViewOptions.all());

            jsonObject = new JSONObject()
                    .put("title", t.getTitle())
                    .put("date", t.getDocumented_at())
                    .put("pages", t.getPages().stream()
                            .map(DtoTranscriptPage::getTranscript)
                            .toList());
        }

        String knowledgeFileId = uploadKnowledgeFile(jsonObject.toString());
        String vectorStoreId = createKnowledgeVector(knowledgeFileId);

        EntityAgent entityAgent = new EntityAgent()
                .setIdFile(IdFile.createIdFile(authService.getUsernameFromContext(), fileId))
                .setVectorStoreId(vectorStoreId)
                .setModel(options.getModel())
                .setInstructions(options.getInstructions())
                .setCreatedAt(OffsetDateTime.now());

        repositoryAgent.save(entityAgent);
        LOG.info("Created agent for fileId {} with vector store {}", fileId, vectorStoreId);
        return entityAgent;
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
        String uploadUrl = "https://api.openai.com/v1/files";

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

    private void saveMessage(IdFile idFile, MessageDir dir, String content) {
        int nextSequence = repositoryAgentMessage
                .findByIdAgentMessage_UsernameAndIdAgentMessage_FileIdOrderByIdAgentMessage_Sequence(idFile.getUsername(), idFile.getFileId())
                .size();

        EntityAgentMessage message = new EntityAgentMessage()
                .setIdAgentMessage(IdAgentMessage.createIdAgentMessage(idFile.getUsername(), idFile.getFileId(), nextSequence))
                .setMessageDir(dir)
                .setContent(content)
                .setCreatedAt(OffsetDateTime.now());

        repositoryAgentMessage.save(message);
    }

    @Override
    public String askAgent(EntityAgent agent, String question) {
        saveMessage(agent.getIdFile(), MessageDir.user, question);

        JSONObject fileSearchTool = new JSONObject()
                .put("type", "file_search")
                .put("vector_store_ids", new JSONArray().put(agent.getVectorStoreId()));

        JSONObject body = new JSONObject()
                .put("model", agent.getModel())
                .put("instructions", agent.getInstructions())
                .put("input", question)
                .put("background", true)
                .put("store", true)
                .put("tools", new JSONArray().put(fileSearchTool));

        if (agent.getLastResponseId() != null) {
            body.put("previous_response_id", agent.getLastResponseId());
        }

        String responseId = postResponses(body);
        agent.setLastResponseId(responseId);
        repositoryAgent.save(agent);

        LOG.info("Created response id {}", responseId);
        return responseId;
    }

    private String postResponses(JSONObject jsonObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonObject.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity("https://api.openai.com/v1/responses", requestEntity, String.class);
        DocumentContext context = JsonPath.parse(response.getBody());
        return context.read("$.id");
    }

    private record ResponseStatus(String status, String outputText) {}

    private ResponseStatus fetchResponse(String responseId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/responses/" + responseId, HttpMethod.GET, requestEntity, String.class);

        String status = JsonPath.read(response.getBody(), "$.status");
        String outputText = status.equals("completed") ? extractOutputText(response.getBody()) : null;
        return new ResponseStatus(status, outputText);
    }

    private String extractOutputText(String responseBody) {
        List<String> texts = JsonPath.read(responseBody, "$.output[?(@.type=='message')].content[0].text");
        return texts.isEmpty() ? "" : texts.get(texts.size() - 1);
    }

    @Override
    public SseEmitter responsePolling(String username, String fileId, String responseId) {
        LOG.info("Starting polling response " + responseId);
        this.emitter = new SseEmitter(600000L);
        IdFile idFile = IdFile.createIdFile(username, fileId);

        final Runnable pollTask = () -> {
            try {
                LOG.info("polling");
                ResponseStatus rs = fetchResponse(responseId);

                if (rs.status().equals("completed")) {
                    LOG.info("completed !");
                    saveMessage(idFile, MessageDir.assistant, rs.outputText());

                    scheduler.shutdown();
                    scheduler = Executors.newScheduledThreadPool(1);

                    this.emitter.send(SseEmitter.event()
                            .name("message")
                            .id("" + lastId++)
                            .data(rs.outputText()));
                } else {
                    LOG.info("still " + rs.status());

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

    private String openAiPostRequest(String path, JSONObject jsonObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        headers.add("OpenAI-Beta", "assistants=v2");

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonObject.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path;

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        DocumentContext context = JsonPath.parse(response.getBody());
        String id = context.read("$.id");
        return id;
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
