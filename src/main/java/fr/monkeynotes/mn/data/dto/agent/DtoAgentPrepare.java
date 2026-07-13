package fr.monkeynotes.mn.data.dto.agent;

import fr.monkeynotes.mn.data.dto.DtoPreferences;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public class DtoAgentPrepare {
        private String model;
        private boolean exists;
        private String instructions;
        private List<DtoAgentMessage> messages;
        private Set<DtoPreferences.AIModel> availableAIModels;
        private String selectedAIModel;
        private OffsetDateTime createdAt;
        //form posted
        private String fileId;
        private String question;
        private boolean reset;

    public String getFileId() {
        return fileId;
    }

    public DtoAgentPrepare setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getQuestion() {
            return question;
        }

        public DtoAgentPrepare setQuestion(String question) {
            this.question = question;
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