package net.kprod.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import net.kprod.mn.data.enums.NamedEntityVerb;

@Entity(name="named_entity")
public class EntityNamedEntity {
    @EmbeddedId
    private IdNamedEntity idNamedEntity;

    @Enumerated(EnumType.STRING)
    private NamedEntityVerb verb;
    private String value;
    private int startIndex;
    private int endIndex;

    public EntityNamedEntity() {
    }

    public IdNamedEntity getIdNamedEntity() {
        return idNamedEntity;
    }

    public EntityNamedEntity setIdNamedEntity(IdNamedEntity idNamedEntity) {
        this.idNamedEntity = idNamedEntity;
        return this;
    }

    public NamedEntityVerb getVerb() {
        return verb;
    }

    public EntityNamedEntity setVerb(NamedEntityVerb verb) {
        this.verb = verb;
        return this;
    }

    public String getValue() {
        return value;
    }

    public EntityNamedEntity setValue(String value) {
        this.value = value;
        return this;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public EntityNamedEntity setStartIndex(int startIndex) {
        this.startIndex = startIndex;
        return this;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public EntityNamedEntity setEndIndex(int endIndex) {
        this.endIndex = endIndex;
        return this;
    }
}
