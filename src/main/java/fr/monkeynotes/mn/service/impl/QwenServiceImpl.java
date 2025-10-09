package fr.monkeynotes.mn.service.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.CompletionResponse;
import fr.monkeynotes.mn.service.PreferencesService;
import fr.monkeynotes.mn.service.QwenService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

@Service
public class QwenServiceImpl implements QwenService {
    private Logger LOG = LoggerFactory.getLogger(QwenService.class);

    @Value("${app.qwen.url}")
    private String qwenApiUrl;

    @Value("${app.qwen.key}")
    private String qwenApiKey;

    @Value("${app.defaults.qwen.model}")
    private String defaultModel;

    @Value("${app.defaults.qwen.prompt}")
    private String defaultPrompt;

    @Value("${app.dry-run:false}")
    private boolean dryRun;

    @Autowired
    private PreferencesService preferencesService;

    private CompletionResponse analyzeImage(String fileId, URL imageURL, String model, String prompt) {
        LOG.info("Qwen request analyse model {} prompt [{}] image {}", model, prompt, imageURL);

        if(dryRun) {
            LOG.warn("Qwen request dry run");
            return new CompletionResponse(fileId, 0, "dryrun", 0, 0, "transcript from " + imageURL + "(dry-run)");
        }

        Optional<Integer> optMaxTokens = Optional.empty();
        try {
            if(preferencesService.useDefaultModelMaxTokens() == false) {
                optMaxTokens = Optional.of(preferencesService.getModelMaxTokens());
            }
        } catch (ServiceException e) {
            LOG.warn("ModelMaxTokens not set", e);
        }

        CompletionResponse completionResponse = new CompletionResponse(fileId);
        long start = System.currentTimeMillis();
        try {
            JSONObject content1_url = new JSONObject();

            content1_url.put("url", imageURL);

            JSONObject content1 = new JSONObject();
            content1.put("type", "image_url");
            content1.put("image_url", content1_url);

            JSONObject content2 = new JSONObject();
            content2.put("type", "text");
            content2.put("text", prompt);

            JSONObject messages = new JSONObject();
            messages.put("role", "user");
            messages.put("content", Arrays.asList(content1, content2));

            JSONObject requestBody = new JSONObject();

            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(messages));

            if(optMaxTokens.isPresent()) {
                requestBody.put("max_tokens", optMaxTokens.get());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenApiKey);

            String respBody;
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

            try {
                ResponseEntity<String> response = createRestTemplate().exchange(qwenApiUrl, HttpMethod.POST, requestEntity, String.class);
                respBody = response.getBody();
            } catch (RuntimeException e) {
                throw new ServiceException("Failed to execute Qwen API", e);
            }
            long took = System.currentTimeMillis() - start;

            DocumentContext context = JsonPath.parse(respBody);

            String content = context.read("$.choices[0].message.content");
            String usedModel = context.read("$.model");
            int completion_tokens = context.read("$.usage.completion_tokens");
            int prompt_tokens = context.read("$.usage.prompt_tokens");

            completionResponse = new CompletionResponse(fileId, took, usedModel, prompt_tokens, completion_tokens, content);

        } catch (Exception e) {
            LOG.error("Failed request model", e);
            completionResponse = completionResponse.failed(fileId, e.getMessage())
                    .setAiModel(model)
                    .setTranscriptTook(System.currentTimeMillis() - start);
        }
        return completionResponse;
    }

    public RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        try {
            if (preferencesService.useDefaultAiConnectTimeout() == false) {
                factory.setConnectTimeout(preferencesService.getAiConnectTimeout()); // milliseconds
            }
        } catch (ServiceException e) {
            LOG.warn("AiConnectTimeout not set", e);
        }
        try {
            if (preferencesService.useDefaultAiReadTimeout() == false) {
                factory.setReadTimeout(preferencesService.getAiReadTimeout()); // milliseconds
            }
        } catch (ServiceException e) {
            LOG.warn("AiReadTimeout not set", e);
        }

        return new RestTemplate(factory);
    }

    @Override
    public CompletionResponse analyzeImage(String fileId, URL imageURL) {

        String model2use = defaultModel;
        String prompt2use = defaultPrompt;
        try {
            model2use = preferencesService.useDefaultModel() ? defaultModel : preferencesService.getModel();
            prompt2use = preferencesService.useDefaultPrompt() ? defaultPrompt : preferencesService.getPrompt();

        } catch (ServiceException e) {
            LOG.warn("Model / Prompt not set", e);
        }
        return analyzeImage(fileId, imageURL, model2use, prompt2use);

    }
}
