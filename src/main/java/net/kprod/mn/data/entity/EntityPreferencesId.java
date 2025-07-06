package net.kprod.mn.data.entity;

import jakarta.persistence.Embeddable;
import net.kprod.mn.data.enums.PreferenceKey;

import java.util.Objects;

@Embeddable
public class EntityPreferencesId {
    private String username;
    private String key;

    public EntityPreferencesId() {
    }

    public static EntityPreferencesId createConfigId(String username, String key) {
        return new EntityPreferencesId()
                .setUsername(username)
                .setKey(key);
    }

    public EntityPreferencesId(String username, PreferenceKey key) {
        this.username = username;
        this.key = key.name();
    }

    public String getUsername() {
        return username;
    }

    public EntityPreferencesId setUsername(String user) {
        this.username = user;
        return this;
    }

    public String getKey() {
        return key;
    }

    public EntityPreferencesId setKey(String key) {
        this.key = key;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EntityPreferencesId that = (EntityPreferencesId) o;
        return Objects.equals(username, that.username) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, key);
    }
}
