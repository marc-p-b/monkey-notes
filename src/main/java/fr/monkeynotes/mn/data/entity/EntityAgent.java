package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;

@Entity(name="agent")
public class EntityAgent {
    @EmbeddedId
    private IdFile idFile;

    private String vectorStoreId;
    private String model;

    @Lob
    private String instructions;

    private String lastResponseId;
    private OffsetDateTime createdAt;

    public EntityAgent() {
    }

    public IdFile getIdFile() {
        return idFile;
    }

    public EntityAgent setIdFile(IdFile idFIle) {
        this.idFile = idFIle;
        return this;
    }

    public String getVectorStoreId() {
        return vectorStoreId;
    }

    public EntityAgent setVectorStoreId(String vectorStoreId) {
        this.vectorStoreId = vectorStoreId;
        return this;
    }

    public String getModel() {
        return model;
    }

    public EntityAgent setModel(String model) {
        this.model = model;
        return this;
    }

    public String getInstructions() {
        return instructions;
    }

    public EntityAgent setInstructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    public String getLastResponseId() {
        return lastResponseId;
    }

    public EntityAgent setLastResponseId(String lastResponseId) {
        this.lastResponseId = lastResponseId;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public EntityAgent setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
