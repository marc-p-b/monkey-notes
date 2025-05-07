package net.kprod.dsb.data.dto.agent;

public class DtoAgentMessage {
        private MessageDir messageDir;
        private String content;

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
    }