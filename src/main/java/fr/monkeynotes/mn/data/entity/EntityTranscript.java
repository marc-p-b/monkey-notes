package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.time.OffsetDateTime;

@Entity(name="transcript")
public class EntityTranscript {
    @EmbeddedId
    private IdFile idFile;
    private String name;
    private OffsetDateTime transcripted_at;
    private OffsetDateTime documented_at;
    private int pageCount;
    private int version;

    public EntityTranscript() {
        this.version = 1;
    }

    public void bumpVersion() {
        this.version++;
    }

    public IdFile getIdFile() {
        return idFile;
    }

    public EntityTranscript setIdFile(IdFile idFile) {
        this.idFile = idFile;
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
