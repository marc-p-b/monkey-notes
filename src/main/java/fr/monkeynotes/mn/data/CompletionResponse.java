package fr.monkeynotes.mn.data;

public class CompletionResponse {
    private String fileId;
    private int pageNumber;

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

    public CompletionResponse(String fileId) {
        this.fileId = fileId;
    }

    public CompletionResponse failed(String fileId, String errorMessage) {
        this.errorMessage = errorMessage;
        this.completed = false;
        return this;
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

    public CompletionResponse setAiModel(String aiModel) {
        this.aiModel = aiModel;
        return this;
    }

    public CompletionResponse setTokensPrompt(int tokensPrompt) {
        this.tokensPrompt = tokensPrompt;
        return this;
    }

    public CompletionResponse setTranscriptTook(long transcriptTook) {
        this.transcriptTook = transcriptTook;
        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public CompletionResponse setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }
}
