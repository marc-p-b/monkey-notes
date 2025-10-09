package fr.monkeynotes.mn.data.dto.agent;

public class DtoAssistantOptions {
        private boolean forceNew;
        private String model;
        private String instructions;

        public boolean isForceNew() {
            return forceNew;
        }

        public DtoAssistantOptions setForceNew(boolean forceNew) {
            this.forceNew = forceNew;
            return this;
        }

        public String getModel() {
            return model;
        }

        public DtoAssistantOptions setModel(String model) {
            this.model = model;
            return this;
        }

        public String getInstructions() {
            return instructions;
        }

        public DtoAssistantOptions setInstructions(String instructions) {
            this.instructions = instructions;
            return this;
        }
    }
