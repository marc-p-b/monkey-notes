package net.kprod.dsb.service.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.kprod.dsb.data.dto.DtoTranscriptPage;
import net.kprod.dsb.service.AgentService;
import net.kprod.dsb.service.ViewService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {
    private Logger LOG = LoggerFactory.getLogger(AgentServiceImpl.class);

    private static final String OPENAI_API_KEY = "";
    private static final String OPENAI_API_URL = "";
    public static final int AGENT_RESPONSE_WAIT_TIMEOUT = 2000;

    @Autowired
    private ViewService viewService;

    @Override
    public String newConversation(String fileId, String question) {
        //using copro

        LOG.info("New agent conversation based on {}", fileId);

        String knowledge = viewService.listTranscriptFromFolderRecurs(fileId)
                .stream()
                .map(t -> new StringBuilder()
                        .append("\ntitle:").append(t.getTitle())
                        .append(", date:").append(t.getDocumented_at())
                        .append(", content:").append(t.getPages().stream()
                                .map(DtoTranscriptPage::getTranscript)
                                .collect(Collectors.joining("\n")))
                        .toString())
                .collect(Collectors.joining("\n"));

        String knowledgeFileId = uploadKnowledgeFile(knowledge);
        String vectorId = createKnowledgeVector(knowledgeFileId);
        String assistantId = createAssistant(vectorId);
        String threadId = createThread();

        LOG.info("Thread created, adding question {}", question);

        addMessage(threadId, question);
        String runId = createRun(assistantId, threadId);

        LOG.info("Return runId {}", runId);
        return "/subscribe/" + threadId + "/" + runId;
    }
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private String uploadKnowledgeFile(String knowledge) {
        // Wrap string as in-memory file
        Resource fileAsResource = new ByteArrayResource(knowledge.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "knowledge.txt"; // Important! Some APIs reject null filename
            }
        };

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(OPENAI_API_KEY);

        // Create body with file part
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileAsResource); // 'file' is the param name expected by the server
        body.add("purpose", "assistants");

        // Build the request
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Send request
        RestTemplate restTemplate = new RestTemplate();
        String uploadUrl = "https://api.openai.com/v1/files"; // Replace with actual URL

        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
        DocumentContext context = JsonPath.parse(response.getBody());
        String fileId = context.read("$.id");

        return fileId;
    }

    private String createKnowledgeVector(String knowledgeFileId) {

        JSONObject jsonObject = new JSONObject()
                .put("name", "knowledge vector store")
                .put("file_ids", Collections.singletonList(knowledgeFileId));

        String vectorId = openAiPostRequest("/v1/vector_stores", jsonObject);

        return vectorId;
    }

    private String createAssistant(String knowledgeVectorId) {
        JSONObject jsonObject = new JSONObject()
                .put("name", "copro doc search")
                .put("instructions", "Tu es un assistant qui répond aux questions en se basant sur le document fourni.")
                .put("model", "gpt-4-turbo")

                .put("tools",
                        new JSONArray().put(new JSONObject().put("type", "file_search")))

                .put("tool_resources",
                        new JSONObject().put("file_search",
                                new JSONObject().put("vector_store_ids", new JSONArray().put(knowledgeVectorId))));


        String assistantId = openAiPostRequest("/v1/assistants", jsonObject);

        return assistantId;
    }

    private String createThread() {
        String threadId = openAiPostRequest("/v1/threads", new JSONObject());

        return threadId;
    }

    private void addMessage(String threadId, String content) {
        JSONObject jsonObject = new JSONObject()
                .put("role", "user")
                .put("content", content);

        openAiPostRequest("/v1/threads/" + threadId + "/messages", jsonObject);
    }

    private String createRun(String assistantId, String threadId) {

        JSONObject jsonObject = new JSONObject()
                .put("assistant_id", assistantId);

        String runId = openAiPostRequest("/v1/threads/" + threadId + "/runs", jsonObject);

        return runId;
    }

    public boolean getRunStatus(String threadId, String runId) {
        String path = "/v1/threads/" + threadId + "/runs/" + runId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(OPENAI_API_KEY);
        headers.add("OpenAI-Beta", "assistants=v2");

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path; // Replace with actual URL

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        DocumentContext context = JsonPath.parse(response.getBody());
        String status = context.read("$.status");
        return status.equals("completed");
    }

    public String getLastResponse(String threadId) {
        String path = "/v1/threads/"+threadId+"/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(OPENAI_API_KEY);
        headers.add("OpenAI-Beta", "assistants=v2");

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com" + path; // Replace with actual URL

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        DocumentContext context = JsonPath.parse(response.getBody());
        String lastResponse = context.read("$.data[0].content[0].text.value");
        return lastResponse;
    }

    private static String openAiPostRequest(String path, JSONObject jsonObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(OPENAI_API_KEY);
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


}
