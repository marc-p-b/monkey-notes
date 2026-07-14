package fr.monkeynotes.mn.data.entity;

import fr.monkeynotes.mn.data.dto.agent.MessageDir;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;

@Entity(name="agent_message")
public class EntityAgentMessage {
    @EmbeddedId
    private IdAgentMessage idAgentMessage;

    private MessageDir messageDir;

    @Lob
    private String content;

    private OffsetDateTime createdAt;

    public IdAgentMessage getIdAgentMessage() {
        return idAgentMessage;
    }

    public EntityAgentMessage setIdAgentMessage(IdAgentMessage idAgentMessage) {
        this.idAgentMessage = idAgentMessage;
        return this;
    }

    public MessageDir getMessageDir() {
        return messageDir;
    }

    public EntityAgentMessage setMessageDir(MessageDir messageDir) {
        this.messageDir = messageDir;
        return this;
    }

    public String getContent() {
        return content;
    }

    public EntityAgentMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public EntityAgentMessage setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
