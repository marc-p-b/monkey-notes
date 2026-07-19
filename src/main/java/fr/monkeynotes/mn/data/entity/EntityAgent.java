package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name="agent")
public class EntityAgent {
    @Id
    private String uuid;

    private String username;
    private String fileIds;
    private String assistantId;
    private String threadId;

    public EntityAgent() {
    }

    public String getUsername() {
        return username;
    }

    public EntityAgent setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getFileIds() {
        return fileIds;
    }

    public EntityAgent setFileIds(String fileIds) {
        this.fileIds = fileIds;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public EntityAgent setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getAssistantId() {
        return assistantId;
    }

    public EntityAgent setAssistantId(String assistantId) {
        this.assistantId = assistantId;
        return this;
    }

    public String getThreadId() {
        return threadId;
    }

    public EntityAgent setThreadId(String threadId) {
        this.threadId = threadId;
        return this;
    }
}
