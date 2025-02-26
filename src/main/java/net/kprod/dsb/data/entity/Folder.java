package net.kprod.dsb.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;

@Entity
public class Folder {
    @Id
    private String fileId;

    @Column(length = 1024)
    private String fileName;

    @Column(length = 1024)
    private String remoteFolder;

    @Column(length = 32)
    private String md5;
    private OffsetDateTime discovered_at;

    public Folder() {
    }

    public String getFileId() {
        return fileId;
    }

    public Folder setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public Folder setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getRemoteFolder() {
        return remoteFolder;
    }

    public Folder setRemoteFolder(String remoteFolder) {
        this.remoteFolder = remoteFolder;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public Folder setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public OffsetDateTime getDiscovered_at() {
        return discovered_at;
    }

    public Folder setDiscovered_at(OffsetDateTime discovered_at) {
        this.discovered_at = discovered_at;
        return this;
    }
}
