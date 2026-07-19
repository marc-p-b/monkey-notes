package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.OffsetDateTime;

@Entity(name="agent")
public class EntityAgent {
    @Id
    private String uuid;

    private String username;
    private String fileIds;
    private String assistantId;

    //TODO indexed
    private String threadId;

    private String threadName;
    private OffsetDateTime createdDate;
    private OffsetDateTime lastUsageDate;

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

    public String getThreadName() {
        return threadName;
    }

    public EntityAgent setThreadName(String threadName) {
        this.threadName = threadName;
        return this;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    public EntityAgent setCreatedDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public OffsetDateTime getLastUsageDate() {
        return lastUsageDate;
    }

    public EntityAgent setLastUsageDate(OffsetDateTime lastUsageDate) {
        this.lastUsageDate = lastUsageDate;
        return this;
    }
}
