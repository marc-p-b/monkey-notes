package net.kprod.dsb.data.dto.agent;

import java.util.List;

public class DtoAgentPrepare {
        private String model;
        private boolean exists;
        private List<DtoAgentMessage> messages;

        public String getModel() {
            return model;
        }

        public DtoAgentPrepare setModel(String model) {
            this.model = model;
            return this;
        }

        public boolean isExists() {
            return exists;
        }

        public DtoAgentPrepare setExists(boolean exists) {
            this.exists = exists;
            return this;
        }

        public List<DtoAgentMessage> getMessages() {
            return messages;
        }

        public DtoAgentPrepare setMessages(List<DtoAgentMessage> messages) {
            this.messages = messages;
            return this;
        }
    }