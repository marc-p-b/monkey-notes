package net.kprod.mn.data.dto.agent;

import java.time.OffsetDateTime;

public class DtoAgentMessage {
        private MessageDir messageDir;
        private String content;
        private OffsetDateTime createdAt;

        public MessageDir getMessageDir() {
            return messageDir;
        }

        public DtoAgentMessage setMessageDir(MessageDir messageDir) {
            this.messageDir = messageDir;
            return this;
        }

        public String getContent() {
            return content;
        }

        public DtoAgentMessage setContent(String content) {
            this.content = content;
            return this;
        }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public DtoAgentMessage setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}