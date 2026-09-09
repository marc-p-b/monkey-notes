package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.entity.EntityTranscriptPageDiff;

import java.time.OffsetDateTime;

/**
 * One stored edit of a page, i.e. one row of transcript_page_diff. The diff itself is deliberately
 * left out — a listing only needs to say which versions exist and when they were written.
 */
public class DtoTranscriptPageDiff {
    private int version;
    private OffsetDateTime createdAt;

    public static DtoTranscriptPageDiff fromEntity(EntityTranscriptPageDiff diff) {
        DtoTranscriptPageDiff dto = new DtoTranscriptPageDiff();
        dto.version = diff.getIdTranscriptPageDiff().getVersion();
        dto.createdAt = diff.getCreatedAt();
        return dto;
    }

    public int getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
