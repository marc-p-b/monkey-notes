package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;

@Entity(name="transcript_page_diff")
public class EntityTranscriptPageDiff {
    @EmbeddedId
    IdTranscriptPageDiff idTranscriptPageDiff;

    @Lob
    private String diff;

    private OffsetDateTime createdAt;

    public IdTranscriptPageDiff getIdTranscriptPageDiff() {
        return idTranscriptPageDiff;
    }

    public EntityTranscriptPageDiff setIdTranscriptPageDiff(IdTranscriptPageDiff idTranscriptPageDiff) {
        this.idTranscriptPageDiff = idTranscriptPageDiff;
        return this;
    }

    public String getDiff() {
        return diff;
    }

    public EntityTranscriptPageDiff setDiff(String diff) {
        this.diff = diff;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public EntityTranscriptPageDiff setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
