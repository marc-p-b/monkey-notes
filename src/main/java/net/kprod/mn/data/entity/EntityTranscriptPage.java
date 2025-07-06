package net.kprod.mn.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity(name="transcript_page")
public class EntityTranscriptPage {
    @EmbeddedId
    IdTranscriptPage idTranscriptPage;

    @Lob
    private String transcript;
    private String aiModel;
    private long transcriptTook;
    private int tokensPrompt;
    private int tokensResponse;
    private int version;
    private boolean completed;

    public EntityTranscriptPage() {
        version = 1;
    }

    public void bumpVersion() {
        version++;
    }

    public IdTranscriptPage getIdTranscriptPage() {
        return idTranscriptPage;
    }

    public EntityTranscriptPage setIdTranscriptPage(IdTranscriptPage idTranscriptPage) {
        this.idTranscriptPage = idTranscriptPage;
        return this;
    }

    public String getTranscript() {
        return transcript;
    }

    public EntityTranscriptPage setTranscript(String transcript) {
        this.transcript = transcript;
        return this;
    }

    public long getTranscriptTook() {
        return transcriptTook;
    }

    public EntityTranscriptPage setTranscriptTook(long transcriptTook) {
        this.transcriptTook = transcriptTook;
        return this;
    }

    public int getTokensPrompt() {
        return tokensPrompt;
    }

    public EntityTranscriptPage setTokensPrompt(int tokensPrompt) {
        this.tokensPrompt = tokensPrompt;
        return this;
    }

    public int getTokensResponse() {
        return tokensResponse;
    }

    public EntityTranscriptPage setTokensResponse(int tokensResponse) {
        this.tokensResponse = tokensResponse;
        return this;
    }

    public String getAiModel() {
        return aiModel;
    }

    public EntityTranscriptPage setAiModel(String aiModel) {
        this.aiModel = aiModel;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public EntityTranscriptPage setVersion(int version) {
        this.version = version;
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }

    public EntityTranscriptPage setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

}
