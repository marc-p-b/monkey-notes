package net.kprod.mn.data.dto;

import net.kprod.mn.data.entity.EntityTranscriptPage;

import java.net.URL;
import java.util.List;

public class DtoTranscriptPage {
    private String fileId;
    private String username;
    private int pageNumber;
    private String transcript;
    private String transcriptHtml;
    private long transcriptTook;
    private int tokensPrompt;
    private int tokensResponse;
    private int version;
    private String aiModel;
    private URL imageUrl;
    private boolean completed;
    private List<DtoNamedEntity> listNamedEntities;

    public static DtoTranscriptPage fromEntity(EntityTranscriptPage page) {
        DtoTranscriptPage dto = new DtoTranscriptPage()
                .setUsername(page.getIdTranscriptPage().getUsername())
                .setFileId(page.getIdTranscriptPage().getFileId())
                .setPageNumber(page.getIdTranscriptPage().getPageNumber())
                .setVersion(page.getVersion())
                .setTranscript(page.getTranscript())
                .setTranscriptHtml(page.getTranscript())
                .setAiModel(page.getAiModel())
                .setTranscriptTook(page.getTranscriptTook())
                .setTokensPrompt(page.getTokensPrompt())
                .setTokensResponse(page.getTokensResponse())
                .setCompleted(page.isCompleted());
        return dto;
    }

    public List<DtoNamedEntity> getListNamedEntities() {
        return listNamedEntities;
    }

    public DtoTranscriptPage setListNamedEntities(List<DtoNamedEntity> listNamedEntities) {
        this.listNamedEntities = listNamedEntities;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DtoTranscriptPage setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public DtoTranscriptPage setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public DtoTranscriptPage setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public String getTranscript() {
        return transcript;
    }

    public DtoTranscriptPage setTranscript(String transcript) {
        this.transcript = transcript;
        return this;
    }

    public String getTranscriptHtml() {
        return transcriptHtml;
    }

    public DtoTranscriptPage setTranscriptHtml(String transcriptHtml) {
        this.transcriptHtml = transcriptHtml;
        return this;
    }

    public long getTranscriptTook() {
        return transcriptTook;
    }

    public DtoTranscriptPage setTranscriptTook(long transcriptTook) {
        this.transcriptTook = transcriptTook;
        return this;
    }

    public int getTokensPrompt() {
        return tokensPrompt;
    }

    public DtoTranscriptPage setTokensPrompt(int tokensPrompt) {
        this.tokensPrompt = tokensPrompt;
        return this;
    }

    public int getTokensResponse() {
        return tokensResponse;
    }

    public DtoTranscriptPage setTokensResponse(int tokensResponse) {
        this.tokensResponse = tokensResponse;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public DtoTranscriptPage setVersion(int version) {
        this.version = version;
        return this;
    }

    public URL getImageUrl() {
        return imageUrl;
    }

    public DtoTranscriptPage setImageUrl(URL imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public String getAiModel() {
        return aiModel;
    }

    public DtoTranscriptPage setAiModel(String aiModel) {
        this.aiModel = aiModel;
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }

    public DtoTranscriptPage setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }
}
