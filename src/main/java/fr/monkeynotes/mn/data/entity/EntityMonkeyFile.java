package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class EntityMonkeyFile {
    private String path;
    private UUID uuid;

    @Id
    public String getPath() {
        return path;
    }

    public EntityMonkeyFile setPath(String path) {
        this.path = path;
        return this;
    }

    public UUID getUuid() {
        return uuid;
    }

    public EntityMonkeyFile setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }
}
