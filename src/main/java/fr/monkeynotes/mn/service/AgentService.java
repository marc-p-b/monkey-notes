package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.agent.DtoAgent;
import fr.monkeynotes.mn.data.dto.agent.DtoAgentPrepare;
import fr.monkeynotes.mn.data.dto.agent.DtoAssistantOptions;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AgentService {
    DtoAgent getOrCreateAssistant(DtoAgentPrepare dtoAgentPrepare, DtoAssistantOptions options);
    DtoAgent newAssistant(DtoAgentPrepare dtoAgentPrepare, DtoAssistantOptions options);
    DtoAgentPrepare prepareAssistant(String fileIds);
    DtoAgentPrepare prepareExistingAssistant(String uuid);
    String createThread();
    void addMessage(String threadId, String content);
    String createRun(DtoAgent agent);
    boolean getRunStatus(String threadId, String runId);
    String getLastResponse(String threadId);
    SseEmitter threadRunPolling(String threadId, String runId);
    List<DtoAgentPrepare> listThreads();
    void setAgentName(String uuid, String agentName);
}
