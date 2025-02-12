package net.kprod.dsb.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.OffsetDateTime;

@Entity
public class Doc {
    @Id
    private String fileId;
    @Column(length = 1024)
    private String fileName;
    @Column(length = 1024)
    private String remoteFolder;
    @Column(length = 32)
    private String md5;
    @Column(length = 1024)
    private String localFolder;
    private OffsetDateTime discovered_at;
    private OffsetDateTime transcripted_at;
    private boolean markForUpdate;

    private String parentFolderName;
    private String parentFolderId;

    public Doc() {
    }

    public Doc(String fileId, String fileName, String remoteFolder, String md5) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.remoteFolder = remoteFolder;
        this.md5 = md5;
        this.markForUpdate = true;
        this.discovered_at = OffsetDateTime.now();
    }

    public String getFileId() {
        return fileId;
    }

    public Doc setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public Doc setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getRemoteFolder() {
        return remoteFolder;
    }

    public Doc setRemoteFolder(String folder) {
        this.remoteFolder = folder;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public Doc setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public OffsetDateTime getDiscovered_at() {
        return discovered_at;
    }

    public Doc setDiscovered_at(OffsetDateTime created_at) {
        this.discovered_at = created_at;
        return this;
    }

    public OffsetDateTime getTranscripted_at() {
        return transcripted_at;
    }

    public Doc setTranscripted_at(OffsetDateTime transcripted_at) {
        this.transcripted_at = transcripted_at;
        return this;
    }

    public boolean isMarkForUpdate() {
        return markForUpdate;
    }

    public Doc setMarkForUpdate(boolean markForUpdate) {
        this.markForUpdate = markForUpdate;
        return this;
    }

    public String getLocalFolder() {
        return localFolder;
    }

    public Doc setLocalFolder(String localFolder) {
        this.localFolder = localFolder;
        return this;
    }

    public String getParentFolderName() {
        return parentFolderName;
    }

    public Doc setParentFolderName(String parentFolderName) {
        this.parentFolderName = parentFolderName;
        return this;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public Doc setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
        return this;
    }
}
