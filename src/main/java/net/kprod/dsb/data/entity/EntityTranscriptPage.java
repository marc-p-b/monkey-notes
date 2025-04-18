package net.kprod.dsb.data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity(name="transcript_page")
public class EntityTranscriptPage {
    @EmbeddedId
    IdTranscriptPage idTranscriptPage;

    @Lob
    private String transcript;
    private long transcriptTook;
    private int tokensPrompt;
    private int tokensResponse;

    public EntityTranscriptPage() {
    }

    public EntityTranscriptPage(IdTranscriptPage idTranscriptPage) {
        this.idTranscriptPage = idTranscriptPage;
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
}
