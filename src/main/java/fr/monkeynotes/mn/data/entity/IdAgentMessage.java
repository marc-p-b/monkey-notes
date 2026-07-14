package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class IdAgentMessage {
    private String fileId;
    private String username;
    private int sequence;

    public IdAgentMessage() {
    }

    public static IdAgentMessage createIdAgentMessage(String username, String fileId, int sequence) {
        IdAgentMessage idAgentMessage = new IdAgentMessage();
        idAgentMessage.username = username;
        idAgentMessage.fileId = fileId;
        idAgentMessage.sequence = sequence;
        return idAgentMessage;
    }

    public String getFileId() {
        return fileId;
    }

    public String getUsername() {
        return username;
    }

    public int getSequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdAgentMessage that = (IdAgentMessage) o;
        return sequence == that.sequence && Objects.equals(fileId, that.fileId) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, username, sequence);
    }
}
