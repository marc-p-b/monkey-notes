package net.kprod.dsb.data.dto;

import net.kprod.dsb.data.entity.EntityConfig;
import net.kprod.dsb.data.entity.EntityConfigId;
import net.kprod.dsb.data.enums.ConfigKey;

public class DtoConfig {
    private String username;
    private ConfigKey key;
    private String value;

    public DtoConfig(String username, ConfigKey key, String value) {
        this.username = username;
        this.key = key;
        this.value = value;
    }

    public static DtoConfig fromEntity(EntityConfig entityConfig) {
        return new DtoConfig(
                entityConfig.getConfigId().getUsername(),
                entityConfig.getConfigId().getKey(),
                entityConfig.getValue()
        );
    }

    public static EntityConfig toEntity(DtoConfig dtoConfig) {
        EntityConfigId entityConfigId = new EntityConfigId(dtoConfig.getUsername(), dtoConfig.getKey());
        return new EntityConfig(entityConfigId, dtoConfig.getValue());
    }

    public String getUsername() {
        return username;
    }

    public DtoConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public ConfigKey getKey() {
        return key;
    }

    public DtoConfig setKey(ConfigKey key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public DtoConfig setValue(String value) {
        this.value = value;
        return this;
    }
}
