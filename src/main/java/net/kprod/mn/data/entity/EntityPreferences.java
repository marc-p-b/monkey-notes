package net.kprod.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity(name="preferences")
public class EntityPreferences {
    private EntityPreferencesId configId;
    private String value;

    public EntityPreferences() {
    }

    public EntityPreferences(EntityPreferencesId configId, String value) {
        this.configId = configId;
        this.value = value;
    }

    @EmbeddedId
    public EntityPreferencesId getConfigId() {
        return configId;
    }

    public EntityPreferences setConfigId(EntityPreferencesId configId) {
        this.configId = configId;
        return this;
    }

    public String getValue() {
        return value;
    }

    public EntityPreferences setValue(String value) {
        this.value = value;
        return this;
    }
}
