package net.kprod.dsb.data.dto;

import net.kprod.dsb.data.entity.EntityTranscript;

import java.time.OffsetDateTime;

public class DtoTranscript {
    private String fileId;
    private String name;
    private OffsetDateTime transcripted_at;
    private OffsetDateTime documented_at;
    private String transcript;
    private long transcriptTook;
    private String aiModel;
    private int tokensPrompt;
    private int tokensResponse;
    private int pageCount;
    private int version;

    public static DtoTranscript fromEntity(EntityTranscript entity) {
        DtoTranscript dto = new DtoTranscript();
        dto.fileId = entity.getFileId();
        dto.name = entity.getName();
        dto.transcripted_at = entity.getTranscripted_at();
        dto.documented_at = entity.getDocumented_at();
        dto.transcript = entity.getTranscript();
        dto.transcriptTook = entity.getTranscriptTook();
        dto.aiModel = entity.getAiModel();
        dto.tokensPrompt = entity.getTokensPrompt();
        dto.tokensResponse = entity.getTokensResponse();
        dto.pageCount = entity.getPageCount();
        dto.version = entity.getVersion();

        return dto;
    }

    public String getFileId() {
        return fileId;
    }

    public String getName() {
        return name;
    }

    public OffsetDateTime getTranscripted_at() {
        return transcripted_at;
    }

    public OffsetDateTime getDocumented_at() {
        return documented_at;
    }

    public String getTranscript() {
        return transcript;
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

    public int getTokensResponse() {
        return tokensResponse;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getVersion() {
        return version;
    }
}
