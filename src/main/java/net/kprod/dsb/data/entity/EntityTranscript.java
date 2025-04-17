package net.kprod.dsb.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;

@Entity(name="transcript")
public class EntityTranscript {
    @Id
    private String fileId;
    private String name;
    private OffsetDateTime transcripted_at;
    private OffsetDateTime documented_at;
    @Lob
    private String aiModel; //todo rename me !!
    private int pageCount;
    private int version;

    public EntityTranscript(String fileId) {
        this.fileId = fileId;
        this.version = 1;
    }

    public EntityTranscript() {
        this.version = 1;
    }

    public String getFileId() {
        return fileId;
    }

    public EntityTranscript setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getName() {
        return name;
    }

    public EntityTranscript setName(String name) {
        this.name = name;
        return this;
    }

    public OffsetDateTime getTranscripted_at() {
        return transcripted_at;
    }

    public EntityTranscript setTranscripted_at(OffsetDateTime transcripted_at) {
        this.transcripted_at = transcripted_at;
        return this;
    }

    public OffsetDateTime getDocumented_at() {
        return documented_at;
    }

    public EntityTranscript setDocumented_at(OffsetDateTime documented_at) {
        this.documented_at = documented_at;
        return this;
    }

    public String getAiModel() {
        return aiModel;
    }

    public EntityTranscript setAiModel(String aiModel) {
        this.aiModel = aiModel;
        return this;
    }

    public int getPageCount() {
        return pageCount;
    }

    public EntityTranscript setPageCount(int pageCount) {
        this.pageCount = pageCount;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public EntityTranscript setVersion(int version) {
        this.version = version;
        return this;
    }
}
