package fr.monkeynotes.mn.data.entity;

import fr.monkeynotes.mn.data.enums.QuickNoteSource;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;

/**
 * A keyboard-typed short note. Unlike a transcript there is no backing PDF, no page images and no
 * OCR, so this deliberately does not reuse EntityTranscript / EntityFile — it only shares the named
 * entity syntax and the search index with them.
 */
@Entity(name="quicknote")
public class EntityQuickNote {
    @EmbeddedId
    private IdQuickNote idQuickNote;

    @Lob
    private String body;

    //derived from a leading markdown heading, or the first line of the body
    @Column(length = 512)
    private String title;

    //note creation date as recorded by the capturing device — what the feed sorts on
    private OffsetDateTime createdAt;

    //server-side write stamp, kept so a later two-way sync can reconcile without a schema change
    private OffsetDateTime updatedAt;

    //tombstone rather than a hard delete, for the same reason
    private OffsetDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    private QuickNoteSource source;

    private int version;

    public EntityQuickNote() {
        this.version = 1;
    }

    public void bumpVersion() {
        this.version++;
    }

    public IdQuickNote getIdQuickNote() {
        return idQuickNote;
    }

    public EntityQuickNote setIdQuickNote(IdQuickNote idQuickNote) {
        this.idQuickNote = idQuickNote;
        return this;
    }

    public String getBody() {
        return body;
    }

    public EntityQuickNote setBody(String body) {
        this.body = body;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public EntityQuickNote setTitle(String title) {
        this.title = title;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public EntityQuickNote setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public EntityQuickNote setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public EntityQuickNote setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
        return this;
    }

    public QuickNoteSource getSource() {
        return source;
    }

    public EntityQuickNote setSource(QuickNoteSource source) {
        this.source = source;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public EntityQuickNote setVersion(int version) {
        this.version = version;
        return this;
    }
}
