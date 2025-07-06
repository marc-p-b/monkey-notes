package net.kprod.mn.service;

import net.kprod.mn.data.dto.agent.DtoAgent;
import net.kprod.mn.data.dto.agent.DtoAgentPrepare;
import net.kprod.mn.data.dto.agent.DtoAssistantOptions;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentService {
    DtoAgent getOrCreateAssistant(String fileId, DtoAssistantOptions options);
    DtoAgent newAssistant(String fileId, DtoAssistantOptions options);
    DtoAgentPrepare prepareAssistant(String assistantId);
    String createThread();
    void addMessage(String threadId, String content);
    String createRun(DtoAgent agent);
    boolean getRunStatus(String threadId, String runId);
    String getLastResponse(String threadId);
    SseEmitter threadRunPolling(String threadId, String runId);
}
