package net.kprod.dsb.data.dto;

import net.kprod.dsb.data.entity.EntityTranscript;
import net.kprod.dsb.data.entity.EntityTranscriptPage;

import java.time.OffsetDateTime;
import java.util.List;

public class DtoTranscript {
    private String username;
    private String fileId;
    private String name;
    private OffsetDateTime transcripted_at;
    private OffsetDateTime documented_at;
    //private String aiModel;
    private int pageCount;
    private int version;
    private List<DtoTranscriptPage> pages;
    private String title;

    public static DtoTranscript fromEntities(EntityTranscript transcript, List<EntityTranscriptPage> pages) {
        DtoTranscript dto = new DtoTranscript();
        dto.username = transcript.getIdFile().getUsername();
        dto.fileId = transcript.getIdFile().getFileId();
        dto.name = transcript.getName();
        dto.title = transcript.getName();
        dto.transcripted_at = transcript.getTranscripted_at();
        dto.documented_at = transcript.getDocumented_at();
        dto.pageCount = transcript.getPageCount();
        dto.version = transcript.getVersion();

        dto.setPages(pages.stream()
                .map(DtoTranscriptPage::fromEntity)
                .toList());

        return dto;
    }

    public String getUsername() {
        return username;
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

    public int getPageCount() {
        return pageCount;
    }

    public int getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public DtoTranscript setTitle(String title) {
        this.title = title;
        return this;
    }

    public DtoTranscript setDocumented_at(OffsetDateTime documented_at) {
        this.documented_at = documented_at;
        return this;
    }

    public List<DtoTranscriptPage> getPages() {
        return pages;
    }

    public DtoTranscript setPages(List<DtoTranscriptPage> pages) {
        this.pages = pages;
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }

    public DtoTranscript setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }
}
