package net.kprod.dsb.data.dto.agent;

import net.kprod.dsb.data.entity.EntityAgent;
import net.kprod.dsb.data.entity.IdFile;

public class DtoAgent {
    private String fileId;
    private String username;
    private String assistantId;
    private String threadId;

    public static DtoAgent of(EntityAgent agent) {
        return new DtoAgent()
                .setFileId(agent.getIdFile().getFileId())
                .setUsername(agent.getIdFile().getUsername())
                .setAssistantId(agent.getAssistantId())
                .setThreadId(agent.getThreadId());
    }

    public static EntityAgent toEntity(DtoAgent agent) {
        return new EntityAgent()
                .setIdFile(IdFile.createIdFile(agent.getUsername(), agent.getFileId()))
                .setAssistantId(agent.getAssistantId())
                .setThreadId(agent.getThreadId());
    }

    public String getFileId() {
        return fileId;
    }

    public DtoAgent setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DtoAgent setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getAssistantId() {
        return assistantId;
    }

    public DtoAgent setAssistantId(String assistantId) {
        this.assistantId = assistantId;
        return this;
    }

    public String getThreadId() {
        return threadId;
    }

    public DtoAgent setThreadId(String threadId) {
        this.threadId = threadId;
        return this;
    }
}
