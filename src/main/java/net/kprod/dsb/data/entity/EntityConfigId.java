package net.kprod.dsb.data.entity;

import jakarta.persistence.Embeddable;
import net.kprod.dsb.data.enums.PreferenceKey;

@Embeddable
public class EntityConfigId {
    private String username;
    private String key;

    public EntityConfigId() {
    }

    public EntityConfigId(String username, PreferenceKey key) {
        this.username = username;
        this.key = key.name();
    }

    public String getUsername() {
        return username;
    }

    public EntityConfigId setUsername(String user) {
        this.username = user;
        return this;
    }

    public String getKey() {
        return key;
    }

    public EntityConfigId setKey(String key) {
        this.key = key;
        return this;
    }
}
