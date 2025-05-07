package net.kprod.dsb.controller;

import net.kprod.dsb.data.dto.DtoAgent;
import net.kprod.dsb.data.dto.DtoURL;
import net.kprod.dsb.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Controller
public class AgentController {
    private Logger LOG = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentService agentService;

    @Value("${app.openai.assistant.defaults.model}")
    private String defaultModelName;

    @Value("${app.openai.assistant.defaults.instructions}")
    private String defaultInstructions;

    public class DtoAssistantOptions {
        private boolean forceNew;
        private String model;
        private String instructions;

        public boolean isForceNew() {
            return forceNew;
        }

        public DtoAssistantOptions setForceNew(boolean forceNew) {
            this.forceNew = forceNew;
            return this;
        }

        public String getModel() {
            return model;
        }

        public DtoAssistantOptions setModel(String model) {
            this.model = model;
            return this;
        }

        public String getInstructions() {
            return instructions;
        }

        public DtoAssistantOptions setInstructions(String instructions) {
            this.instructions = instructions;
            return this;
        }
    }

    @PostMapping("/agent/ask")
    public ResponseEntity<DtoURL> agentStreamLink(@RequestParam Map<String, String> formData) {
        String question = formData.get("question");
        String fileId = formData.get("fileId");
        boolean forceNewAssistant = Boolean.parseBoolean(formData.get("forceNewAssistant"));
        boolean forceNewThread = Boolean.parseBoolean(formData.get("forceNewThread"));
        String newAssistantInstructions = formData.get("newAssistantInstructions");
        String newAssistantModel = formData.get("newAssistantModel");

        if(newAssistantModel == null || newAssistantModel.isEmpty()) {
            newAssistantModel = defaultModelName;
        }

        if(newAssistantInstructions == null || newAssistantInstructions.isEmpty()) {
            newAssistantInstructions = defaultInstructions;
        }

        DtoAssistantOptions dtoOptions = new DtoAssistantOptions()
                .setForceNew(forceNewAssistant)
                .setModel(newAssistantModel)
                .setInstructions(newAssistantInstructions);

        DtoAgent dtoAgent = agentService.getOrCreateAssistant(fileId, dtoOptions);
        agentService.addMessage(dtoAgent.getThreadId(), question);
        String runId = agentService.createRun(dtoAgent);
        String streamLink = "/subscribe/" + dtoAgent.getThreadId() + "/" + runId;

        return ResponseEntity.ok().body(new DtoURL(streamLink));
    }


    @GetMapping("/subscribe/{threadId}/{runId}")
    public SseEmitter subscribe(@PathVariable String threadId, @PathVariable String runId) throws IOException {
        return agentService.threadRunPolling(threadId, runId);
    }

}