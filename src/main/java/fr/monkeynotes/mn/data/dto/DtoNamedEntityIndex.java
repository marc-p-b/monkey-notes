package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.entity.EntityNamedEntityIndex;
import fr.monkeynotes.mn.data.enums.NamedEntityVerb;

import java.util.Objects;

public class DtoNamedEntityIndex {
    private NamedEntityVerb verb;
    private String value;
    private String username;
    private long count;

    public static DtoNamedEntityIndex fromEntity(EntityNamedEntityIndex entity) {
        return new DtoNamedEntityIndex()
                .setUsername(entity.getIdNamedEntityIndex().getUsername())
                .setVerb(entity.getIdNamedEntityIndex().getVerb())
                .setValue(entity.getIdNamedEntityIndex().getValue());
    }

    public String getUsername() {
        return username;
    }

    public DtoNamedEntityIndex setUsername(String username) {
        this.username = username;
        return this;
    }

    public NamedEntityVerb getVerb() {
        return verb;
    }

    public DtoNamedEntityIndex setVerb(NamedEntityVerb verb) {
        this.verb = verb;
        return this;
    }

    public String getValue() {
        return value;
    }

    public DtoNamedEntityIndex setValue(String value) {
        this.value = value;
        return this;
    }

    public long getCount() {
        return count;
    }

    public DtoNamedEntityIndex setCount(long count) {
        this.count = count;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DtoNamedEntityIndex that = (DtoNamedEntityIndex) o;
        return verb == that.verb && Objects.equals(value, that.value) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(verb, value, username);
    }
}