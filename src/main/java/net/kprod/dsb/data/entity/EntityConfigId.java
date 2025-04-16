package net.kprod.dsb.data.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import net.kprod.dsb.data.enums.ConfigKey;

@Embeddable
public class EntityConfigId {
    private String username;
    private ConfigKey key;

    public EntityConfigId() {
    }

    public EntityConfigId(String username, ConfigKey key) {
        this.username = username;
        this.key = key;
    }

    public String getUsername() {
        return username;
    }

    public EntityConfigId setUsername(String user) {
        this.username = user;
        return this;
    }

    @Enumerated(EnumType.STRING)
    public ConfigKey getKey() {
        return key;
    }

    public EntityConfigId setKey(ConfigKey key) {
        this.key = key;
        return this;
    }
}
