package net.kprod.dsb.data.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class EntityConfig {
    private EntityConfigId configId;
    private String value;

    public EntityConfig() {
    }

    public EntityConfig(EntityConfigId configId, String value) {
        this.configId = configId;
        this.value = value;
    }

    @EmbeddedId
    public EntityConfigId getConfigId() {
        return configId;
    }

    public EntityConfig setConfigId(EntityConfigId configId) {
        this.configId = configId;
        return this;
    }

    public String getValue() {
        return value;
    }

    public EntityConfig setValue(String value) {
        this.value = value;
        return this;
    }
}
