package net.kprod.dsb.service.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.CompletionResponse;
import net.kprod.dsb.service.QwenService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Service
public class QwenServiceImpl implements QwenService {
    private Logger LOG = LoggerFactory.getLogger(QwenService.class);

    @Value("${app.qwen.url}")
    private String qwenApiUrl;

    @Value("${app.qwen.key}")
    private String qwenApiKey;

    @Value("${app.qwen.model}")
    private String qwenModel;

    @Value("${app.qwen.prompt}")
    private String qwenPrompt;

    @Value("${app.url.self}")
    private String appHost;

    @Value("${app.dry-run:false}")
    private boolean dryRun;

    @Override
    public CompletionResponse analyzeImage(String fileId, URL imageURL, String model, String prompt) {
        LOG.info("Qwen request analyse model {} prompt [{}] image {}", model, prompt, imageURL);

        if(dryRun) {
            LOG.warn("Qwen request dry run");
            return new CompletionResponse(fileId, 0, "dryrun", 0, 0, "transcript from " + imageURL + "(dry-run)");
        }

        CompletionResponse completionResponse = null;
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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenApiKey);

            //LOG.info("debug json {}", requestBody.toString());

            String respBody;
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            long start = System.currentTimeMillis();
            try {
                ResponseEntity<String> response = new RestTemplate().exchange(qwenApiUrl, HttpMethod.POST, requestEntity, String.class);
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
            completionResponse = CompletionResponse.failed(fileId, e.getMessage());
        }
        return completionResponse;
    }

    @Override
    public CompletionResponse analyzeImage(String fileId, URL imageURL) {
        return analyzeImage(fileId, imageURL, qwenModel, qwenPrompt);
    }
}
