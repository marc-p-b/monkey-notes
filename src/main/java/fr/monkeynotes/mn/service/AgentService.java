package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.agent.DtoAgentPrepare;
import fr.monkeynotes.mn.data.dto.agent.DtoAssistantOptions;
import fr.monkeynotes.mn.data.entity.EntityAgent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentService {
    EntityAgent getOrCreateAgent(String fileId, DtoAssistantOptions options);
    EntityAgent newAgent(String fileId, DtoAssistantOptions options);
    DtoAgentPrepare prepareAssistant(String fileId);
    String askAgent(EntityAgent agent, String question);
    SseEmitter responsePolling(String username, String fileId, String responseId);
}
