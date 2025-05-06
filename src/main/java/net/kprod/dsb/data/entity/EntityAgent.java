package net.kprod.dsb.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class EntityAgent {
    @EmbeddedId
    private IdFile idFile;

    private String assistantId;
    private String threadId;

    public EntityAgent() {
    }

    public IdFile getIdFile() {
        return idFile;
    }

    public EntityAgent setIdFile(IdFile idFIle) {
        this.idFile = idFIle;
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
