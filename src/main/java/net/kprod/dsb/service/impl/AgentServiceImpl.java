package net.kprod.dsb.service.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.kprod.dsb.controller.AgentController;
import net.kprod.dsb.data.dto.DtoAgent;
import net.kprod.dsb.data.dto.DtoTranscriptPage;
import net.kprod.dsb.data.entity.EntityAgent;
import net.kprod.dsb.data.entity.IdFile;
import net.kprod.dsb.data.repository.RepositoryAgent;
import net.kprod.dsb.service.AgentService;
import net.kprod.dsb.service.AuthService;
import net.kprod.dsb.service.ViewService;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {
    private Logger LOG = LoggerFactory.getLogger(AgentServiceImpl.class);

    @Value("${app.openai.api}")
    private String openAiApiKey;

    @Autowired
    private ViewService viewService;

    @Autowired
    private RepositoryAgent repositoryAgent;

    @Autowired
    private AuthService authService;

    //make this multiuser
    private SseEmitter emitter;
    private Long lastId = 0L;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public DtoAgent getOrCreateAssistant(String fileId, AgentController.DtoAssistantOptions options) {
        if(options.isForceNew() == true) {
            return this.newAssistant(fileId, options);
        } else {
            Optional<EntityAgent> agent = repositoryAgent.findById(IdFile.createIdFile(authService.getConnectedUsername(), fileId));
            if(agent.isPresent()) {
                return DtoAgent.of(agent.get());
            } else {
                return this.newAssistant(fileId, options);
            }
        }
    }

    @Override
    public DtoAgent newAssistant(String fileId, AgentController.DtoAssistantOptions options) {
        LOG.info("New agent based on fileId {}", fileId);

        List<JSONObject> jsonList = viewService.listTranscriptFromFolderRecurs(fileId)
                .stream()
                .map(t -> {
                    return new JSONObject()
                            .put("title", t.getTitle())
                            .put("date", t.getDocumented_at())
                            .put("pages", t.getPages().stream()
                                    .map(DtoTranscriptPage::getTranscript)
                                    .toList());
                })
                .toList();

        JSONObject jsonObject = new JSONObject()
                .put("documents", jsonList);

        String knowledgeFileId = uploadKnowledgeFile(jsonObject.toString());
        String vectorId = createKnowledgeVector(knowledgeFileId);

        //add title
        String name = "knowledge fileId " + fileId;
        String assistantId = createAssistant(name, options.getInstructions(), options.getModel(), vectorId);
        String threadId = createThread();
        LOG.info("Create assistant id {} threadId {}", assistantId, threadId);

        EntityAgent entityAgent = new EntityAgent()
                .setAssistantId(assistantId)
                .setThreadId(threadId)
                .setIdFile(IdFile.createIdFile(authService.getConnectedUsername(), fileId));

        repositoryAgent.save(entityAgent);
        return DtoAgent.of(entityAgent);
    }

    private String uploadKnowledgeFile(String knowledge) {
        Resource fileAsResource = new ByteArrayResource(knowledge.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "knowledge.json";
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

    //todo when no polling is active ?
    @Scheduled(fixedRate = 30000)
    public void heartbeat() throws IOException {
        this.emitter.send(SseEmitter.event()
                .name("message")
                .id("" + ++lastId)
                .data("heartbeat"));
    }


}
