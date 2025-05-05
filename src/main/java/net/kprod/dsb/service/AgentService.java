package net.kprod.dsb.service;

public interface AgentService {
    String newConversation(String fileId, String question);
    boolean getRunStatus(String threadId, String runId);
    String getLastResponse(String threadId);
}
