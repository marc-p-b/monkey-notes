package net.kprod.dsb.service;

import net.kprod.dsb.controller.AgentController;
import net.kprod.dsb.data.dto.DtoAgent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentService {
    DtoAgent getOrCreateAssistant(String fileId, AgentController.DtoAssistantOptions options);
    DtoAgent newAssistant(String fileId, AgentController.DtoAssistantOptions options);
    String createThread();
    void addMessage(String threadId, String content);
    String createRun(DtoAgent agent);
    boolean getRunStatus(String threadId, String runId);
    String getLastResponse(String threadId);
    SseEmitter threadRunPolling(String threadId, String runId);
}
