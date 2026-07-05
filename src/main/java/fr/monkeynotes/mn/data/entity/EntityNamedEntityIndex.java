package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.time.OffsetDateTime;

@Entity(name="named_entity_index")
public class EntityNamedEntityIndex {
    @EmbeddedId
    private IdNamedEntityIndex idNamedEntityIndex;

    private OffsetDateTime createdAt;

    public EntityNamedEntityIndex() {
    }

    public IdNamedEntityIndex getIdNamedEntityIndex() {
        return idNamedEntityIndex;
    }

    public EntityNamedEntityIndex setIdNamedEntityIndex(IdNamedEntityIndex idNamedEntityIndex) {
        this.idNamedEntityIndex = idNamedEntityIndex;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public EntityNamedEntityIndex setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

}
