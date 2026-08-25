package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.entity.EntityQuickNote;
import fr.monkeynotes.mn.data.enums.QuickNoteSource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DtoQuickNote {
    private UUID uuid;
    private String body;
    private String title;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private QuickNoteSource source;

    //populated on the read paths only — the frontend needs the start/end offsets to render tags and
    //people inline, the same way it does for a transcript page
    private List<DtoNamedEntity> listNamedEntities;

    public static DtoQuickNote fromEntity(EntityQuickNote entity) {
        return new DtoQuickNote()
                .setUuid(entity.getIdQuickNote().getUuid())
                .setBody(entity.getBody())
                .setTitle(entity.getTitle())
                .setCreatedAt(entity.getCreatedAt())
                .setUpdatedAt(entity.getUpdatedAt())
                .setSource(entity.getSource());
    }

    public UUID getUuid() {
        return uuid;
    }

    public DtoQuickNote setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getBody() {
        return body;
    }

    public DtoQuickNote setBody(String body) {
        this.body = body;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public DtoQuickNote setTitle(String title) {
        this.title = title;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public DtoQuickNote setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public DtoQuickNote setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public QuickNoteSource getSource() {
        return source;
    }

    public DtoQuickNote setSource(QuickNoteSource source) {
        this.source = source;
        return this;
    }

    public List<DtoNamedEntity> getListNamedEntities() {
        return listNamedEntities;
    }

    public DtoQuickNote setListNamedEntities(List<DtoNamedEntity> listNamedEntities) {
        this.listNamedEntities = listNamedEntities;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DtoQuickNote that = (DtoQuickNote) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
