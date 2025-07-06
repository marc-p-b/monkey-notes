package net.kprod.mn.data.entity;

import jakarta.persistence.*;
import net.kprod.mn.data.enums.FileType;

import java.time.OffsetDateTime;

@Entity(name="file")
public class EntityFile {
    @EmbeddedId
    private IdFile idFile;

    private String parentFolderId;
    @Column(length = 1024)
    private String name;
    @Column(length = 32)
    private String md5;
    private OffsetDateTime discovered_at;
    @Enumerated(EnumType.STRING)
    private FileType type;

    public EntityFile() {
        this.discovered_at = OffsetDateTime.now();
    }

    public IdFile getIdFile() {
        return idFile;
    }

    public EntityFile setIdFile(IdFile idFile) {
        this.idFile = idFile;
        return this;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public EntityFile setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
        return this;
    }

    public String getName() {
        return name;
    }

    public EntityFile setName(String fileName) {
        this.name = fileName;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public EntityFile setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public OffsetDateTime getDiscovered_at() {
        return discovered_at;
    }

    public EntityFile setDiscovered_at(OffsetDateTime discovered_at) {
        this.discovered_at = discovered_at;
        return this;
    }

    public FileType getType() {
        return type;
    }

    public EntityFile setType(FileType type) {
        this.type = type;
        return this;
    }
}
