import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class TestQwen {
    //https://dashscope-intl.aliyuncs.com/compatible-mode/v1
    //https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
    private static final String API_URL = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String API_KEY = "sk-2f128fcd413245e2a1e4c9f11b437620"; // Replace with your DashScope API Key
    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testQwen() {
        String img = "https://ekladata.com/zXQokykmXBepmvGuod0p84v3_k4.jpg";
        analyzeImage(img, "image to text");
    }

    //curl --location 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions' \
    //--header "Authorization: Bearer sk-2f128fcd413245e2a1e4c9f11b437620" \
    //--header 'Content-Type: application/json' \
    //--data '{
    //  "model": "qwen-vl-max",
    //  "messages": [{
    //    "role": "user",
    //    "content": [
    //      {"type": "image_url", "image_url": {"url": "https://ekladata.com/zXQokykmXBepmvGuod0p84v3_k4.jpg"}},
    //      {"type": "text", "text": "this is a french manuscript, extract the text"}
    //    ]
    //  }]
    //}'

    public String analyzeImage(String imagePath, String prompt) {
        try {
            JSONObject content1_url = new JSONObject();
            //content1_url.put("url", "https://ekladata.com/zXQokykmXBepmvGuod0p84v3_k4.jpg");
            content1_url.put("url", "https://drive.google.com/uc?export=view&id=1fHTiJM_hsr571Q8v_7zYxKBoDX8jdH1P");


            JSONObject content1 = new JSONObject();
            content1.put("type", "image_url");
            content1.put("image_url", content1_url);


            JSONObject content2 = new JSONObject();
            content2.put("type", "text");
            content2.put("text", "this is a handwritten french extract text place result between brackets");

            JSONObject messages = new JSONObject();
            messages.put("role", "user");
            messages.put("content", Arrays.asList(content1, content2));


            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "qwen2.5-vl-72b-instruct"); //qwen2.5-vl-72b-instruct //qwen-vl-max
            requestBody.put("messages", Arrays.asList(messages));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + API_KEY);

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

            // Send POST request
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, requestEntity, String.class);

            String respBody = response.getBody();//"{\"choices\":[{\"message\":{\"content\":\"[Depuis le 1er juin 2015, un ordinateur équipé d'eye tracking a été prêté par la société Tobii Dynavox au CHU de Tours (Hôpital Bretonneau, service réanimation) pour être testé auprès d'une dizaine de patients. Les membres du personnel soignant et l'équipe paramédicale ont trouvé le système très utile, explique le Dr Bodet-Contentin.]\",\"role\":\"assistant\"},\"finish_reason\":\"stop\",\"index\":0,\"logprobs\":null}],\"object\":\"chat.completion\",\"usage\":{\"prompt_tokens\":410,\"completion_tokens\":97,\"total_tokens\":507},\"created\":1739738723,\"system_fingerprint\":null,\"model\":\"qwen2.5-vl-72b-instruct\",\"id\":\"chatcmpl-0bbcad38-9e01-90ea-b30d-1edd8a48021a\"}";
            DocumentContext context = JsonPath.parse(respBody);

            String content = context.read("$.choices[0].message.content");
            Pattern p = Pattern.compile("^\\[(.*)]$");
            Matcher m = p.matcher(content);
            if (m.find()) {
                content = m.group(1);
            }

            return content;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }

    }
}