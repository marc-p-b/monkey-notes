package net.kprod.dsb.data;

public class CompletionResponse {
    private String fileId;
    private long transcriptTook;
    private String aiModel;
    private int tokensPrompt;
    private int tokensCompletion;
    private String transcript;

    public CompletionResponse(String fileId, long transcriptTook, String aiModel, int tokensPrompt, int tokensCompletion, String transcript) {
        this.fileId = fileId;
        this.transcriptTook = transcriptTook;
        this.aiModel = aiModel;
        this.tokensPrompt = tokensPrompt;
        this.tokensCompletion = tokensCompletion;
        this.transcript = transcript;
    }

    public String getFileId() {
        return fileId;
    }

    public long getTranscriptTook() {
        return transcriptTook;
    }

    public String getAiModel() {
        return aiModel;
    }

    public int getTokensPrompt() {
        return tokensPrompt;
    }

    public int getTokensCompletion() {
        return tokensCompletion;
    }

    public String getTranscript() {
        return transcript;
    }
}
