package net.kprod.dsb.data;

public class CompletionResponse {
    private String fileId;
    private long transcriptTook;
    private String aiModel;
    private int tokensPrompt;
    private int tokensCompletion;
    private String transcript;
    private File2Process file2Process;
    private boolean completed = true;
    private String errorMessage;

    public CompletionResponse(String fileId, long transcriptTook, String aiModel, int tokensPrompt, int tokensCompletion, String transcript) {
        this.fileId = fileId;
        this.transcriptTook = transcriptTook;
        this.aiModel = aiModel;
        this.tokensPrompt = tokensPrompt;
        this.tokensCompletion = tokensCompletion;
        this.transcript = transcript;
    }

    private CompletionResponse(String fileId) {
        this.fileId = fileId;
    }

    public static CompletionResponse failed(String fileId, String errorMessage) {
        CompletionResponse failedResponse = new CompletionResponse(fileId);
        failedResponse.fileId = fileId;
        failedResponse.errorMessage = errorMessage;
        failedResponse.completed = false;
        return failedResponse;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getErrorMessage() {
        return errorMessage;
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
