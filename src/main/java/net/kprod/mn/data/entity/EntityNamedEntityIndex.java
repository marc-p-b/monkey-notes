package net.kprod.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import net.kprod.mn.data.enums.NamedEntityVerb;

import java.time.OffsetDateTime;

@Entity(name="named_entity_index")
public class EntityNamedEntityIndex {
    @EmbeddedId
    private IdNamedEntityIndex idNamedEntityIndex;

    private OffsetDateTime createdAt;
    //private OffsetDateTime updatedAt;

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

//    public OffsetDateTime getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public EntityNamedEntityIndex setUpdatedAt(OffsetDateTime updatedAt) {
//        this.updatedAt = updatedAt;
//        return this;
//    }
}
