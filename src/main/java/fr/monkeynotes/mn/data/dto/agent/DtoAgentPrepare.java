package fr.monkeynotes.mn.data.dto.agent;

import fr.monkeynotes.mn.data.dto.DtoPreferences;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public class DtoAgentPrepare {
        private String uuid;
        private String model;
        private boolean exists;
        private String instructions;
        private String threadName;
        private List<DtoAgentMessage> messages;
        private Set<DtoPreferences.AIModel> availableAIModels;
        private String selectedAIModel;
        private OffsetDateTime createdAt;
        //form posted
        private Set<String> fileIds;
        private boolean reset;

        public String getUuid() {
            return uuid;
        }

        public DtoAgentPrepare setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public String getThreadName() {
            return threadName;
        }

        public DtoAgentPrepare setThreadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Set<String> getFileIds() {
            return fileIds;
        }

        public DtoAgentPrepare setFileIds(Set<String> fileIds) {
            this.fileIds = fileIds;
            return this;
        }

        public boolean isReset() {
            return reset;
        }

        public DtoAgentPrepare setReset(boolean reset) {
            this.reset = reset;
            return this;
        }

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

    public String getInstructions() {
        return instructions;
    }

    public DtoAgentPrepare setInstructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public DtoAgentPrepare setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Set<DtoPreferences.AIModel> getAvailableAIModels() {
        return availableAIModels;
    }

    public DtoAgentPrepare setAvailableAIModels(Set<DtoPreferences.AIModel> availableAIModels) {
        this.availableAIModels = availableAIModels;
        return this;
    }

    public String getSelectedAIModel() {
        return selectedAIModel;
    }

    public DtoAgentPrepare setSelectedAIModel(String selectedAIModel) {
        this.selectedAIModel = selectedAIModel;
        return this;
    }
}