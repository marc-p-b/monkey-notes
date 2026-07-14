package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.JwtUtil;
import fr.monkeynotes.mn.data.dto.DtoURL;
import fr.monkeynotes.mn.data.dto.agent.DtoAgentPrepare;
import fr.monkeynotes.mn.data.dto.agent.DtoAssistantOptions;
import fr.monkeynotes.mn.data.entity.EntityAgent;
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

@Controller
public class AgentController {
    private Logger LOG = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentService agentService;

    @Value("${app.openai.models.default}")
    private String defaultModelName;

    @Value("${app.openai.models.instructions}")
    private String defaultInstructions;

    @GetMapping("/agent/prepare/{fileId}")
    public ResponseEntity<DtoAgentPrepare> agentPrepare(@PathVariable String fileId) {
        return ResponseEntity.ok(agentService.prepareAssistant(fileId));
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

        EntityAgent agent = agentService.getOrCreateAgent(agentPrepare.getFileId(), dtoOptions);
        //getlast() safe ?
        String responseId = agentService.askAgent(agent, agentPrepare.getMessages().getLast().getContent());
        String streamLink = "agent/subscribe/" + agentPrepare.getFileId() + "/" + responseId;

        return ResponseEntity.ok().body(new DtoURL(streamLink));

    }

    @GetMapping("/agent/subscribe/{fileId}/{responseId}/{token}")
    public SseEmitter subscribe(@PathVariable String fileId, @PathVariable String responseId, @PathVariable String token) throws IOException {

        if(JwtUtil.validateToken(token) == false) {
            LOG.error("Invalid token");
            return null;
        }

        String username = JwtUtil.extractUsername(token);
        return agentService.responsePolling(username, fileId, responseId);
    }

}