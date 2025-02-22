package net.kprod.dsb.data;

public class CompletionResponse {
    private String fileId;
    private long transcriptTook;
    private String aiModel;
    private int tokensPrompt;
    private int tokensCompletion;
    private String transcript;
    private File2Process file2Process;

    public CompletionResponse(String fileId, long transcriptTook, String aiModel, int tokensPrompt, int tokensCompletion, String transcript) {
        this.fileId = fileId;
        this.transcriptTook = transcriptTook;
        this.aiModel = aiModel;
        this.tokensPrompt = tokensPrompt;
        this.tokensCompletion = tokensCompletion;
        this.transcript = transcript;
    }

    public File2Process getFile2Process() {
        return file2Process;
    }

    public CompletionResponse setFile2Process(File2Process file2Process) {
        this.file2Process = file2Process;
        return this;
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
