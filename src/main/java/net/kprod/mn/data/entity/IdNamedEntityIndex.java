package net.kprod.mn.data.entity;

import jakarta.persistence.*;
import net.kprod.mn.data.enums.NamedEntityVerb;

import java.util.Objects;

@Embeddable
public class IdNamedEntityIndex {
    @Enumerated(EnumType.STRING)
    private NamedEntityVerb verb;
    private String value;
    private String username;

    public IdNamedEntityIndex() {
    }

    public static IdNamedEntityIndex createIdNamedEntityIndex(String username, NamedEntityVerb verb, String value) {
        return new IdNamedEntityIndex()
                .setVerb(verb)
                .setValue(value)
                .setUsername(username);
    }

    public NamedEntityVerb getVerb() {
        return verb;
    }

    public IdNamedEntityIndex setVerb(NamedEntityVerb verb) {
        this.verb = verb;
        return this;
    }

    public String getValue() {
        return value;
    }

    public IdNamedEntityIndex setValue(String value) {
        this.value = value;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public IdNamedEntityIndex setUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdNamedEntityIndex that = (IdNamedEntityIndex) o;
        return verb == that.verb && Objects.equals(value, that.value) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(verb, value, username);
    }

}
