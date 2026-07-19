package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.JwtUtil;
import fr.monkeynotes.mn.data.dto.DtoURL;
import fr.monkeynotes.mn.data.dto.agent.DtoAgent;
import fr.monkeynotes.mn.data.dto.agent.DtoAgentPrepare;
import fr.monkeynotes.mn.data.dto.agent.DtoAssistantOptions;
import fr.monkeynotes.mn.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Controller
public class AgentController {
    private Logger LOG = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentService agentService;

    @Value("${app.openai.models.default}")
    private String defaultModelName;

    @Value("${app.openai.models.instructions}")
    private String defaultInstructions;

    @GetMapping("/agent/prepare")
    public ResponseEntity<DtoAgentPrepare> agentPrepare(@RequestParam String fileIds) {
        return ResponseEntity.ok(agentService.prepareAssistant(fileIds));
    }

    @PostMapping("/agent/ask")
    public ResponseEntity<DtoURL> agentStreamLink(@RequestBody DtoAgentPrepare agentPrepare) {
        String newAssistantInstructions = agentPrepare.getInstructions();
        String newAssistantModel = agentPrepare.getModel();

        if(newAssistantModel == null || newAssistantModel.isEmpty()) {
            newAssistantModel = defaultModelName;
        }

        if(newAssistantInstructions == null || newAssistantInstructions.isEmpty()) {
            newAssistantInstructions = defaultInstructions;
        }

        DtoAssistantOptions dtoOptions = new DtoAssistantOptions()
                .setForceNew(agentPrepare.isReset())
                .setModel(newAssistantModel)
                .setInstructions(newAssistantInstructions);

        DtoAgent dtoAgent = agentService.getOrCreateAssistant(agentPrepare, dtoOptions);
        //agentService.addMessage(dtoAgent.getThreadId(), agentPrepare.getQuestion());
        //getlast() safe ?
        agentService.addMessage(dtoAgent.getThreadId(), agentPrepare.getMessages().getLast().getContent());
        String runId = agentService.createRun(dtoAgent);
        String streamLink = "agent/subscribe/" + dtoAgent.getThreadId() + "/" + runId;

        return ResponseEntity.ok().body(new DtoURL(streamLink));

    }

    @GetMapping("/agent/subscribe/{threadId}/{runId}/{token}")
    public SseEmitter subscribe(@PathVariable String threadId, @PathVariable String runId, @PathVariable String token) throws IOException {

        if(JwtUtil.validateToken(token) == false) {
            LOG.error("Invalid token");
            return null;
        }

        return agentService.threadRunPolling(threadId, runId);
    }

    @GetMapping("/agent/list")
    public ResponseEntity<List<DtoAgentPrepare>> agentList() {
        return ResponseEntity.ok(agentService.listThreads());
    }

}