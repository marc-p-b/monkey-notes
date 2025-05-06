package net.kprod.dsb.service;

import net.kprod.dsb.data.dto.DtoAgent;

public interface AgentService {
    DtoAgent getOrCreateAssistant(String fileId, boolean forceCreate);
    DtoAgent newAssistant(String fileId);
    String createThread();
    void addMessage(String threadId, String content);
    String createRun(DtoAgent agent);
    boolean getRunStatus(String threadId, String runId);
    String getLastResponse(String threadId);
}
