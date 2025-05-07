package net.kprod.dsb.data.entity;

import jakarta.persistence.Embeddable;
import net.kprod.dsb.data.enums.PreferenceKey;

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
}
