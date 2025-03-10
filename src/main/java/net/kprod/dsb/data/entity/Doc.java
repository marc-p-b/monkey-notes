package net.kprod.dsb.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import org.hibernate.annotations.Type;

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
    private OffsetDateTime discovered_at;
    private OffsetDateTime transcripted_at;
    private OffsetDateTime documented_at;

    @Lob
    private String transcript;
    private String parentFolderName;
    private String parentFolderId;
    private long transcriptTook;
    private String aiModel;
    private int tokensPrompt;
    private int tokensResponse;
    private int pageCount;
    private int version;

    public Doc() {
        this.discovered_at = OffsetDateTime.now();
        this.version = 1;
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

    public String getTranscript() {
        return transcript;
    }

    public Doc setTranscript(String text) {
        this.transcript = text;
        return this;
    }

    public long getTranscriptTook() {
        return transcriptTook;
    }

    public Doc setTranscriptTook(long transcriptTook) {
        this.transcriptTook = transcriptTook;
        return this;
    }

    public String getAiModel() {
        return aiModel;
    }

    public Doc setAiModel(String aiModel) {
        this.aiModel = aiModel;
        return this;
    }

    public long getTokensPrompt() {
        return tokensPrompt;
    }

    public Doc setTokensPrompt(int tokensPrompt) {
        this.tokensPrompt = tokensPrompt;
        return this;
    }

    public long getTokensResponse() {
        return tokensResponse;
    }

    public Doc setTokensResponse(int tokensResponse) {
        this.tokensResponse = tokensResponse;
        return this;
    }

    public int getPageCount() {
        return pageCount;
    }

    public Doc setPageCount(int pagerCount) {
        this.pageCount = pagerCount;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public Doc setVersion(int version) {
        this.version = version;
        return this;
    }

    public OffsetDateTime getDocumented_at() {
        return documented_at;
    }

    public Doc setDocumented_at(OffsetDateTime documented_at) {
        this.documented_at = documented_at;
        return this;
    }
}
