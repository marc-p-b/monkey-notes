package net.kprod.dsb.data.dto;

import net.kprod.dsb.data.entity.EntityTranscript;
import net.kprod.dsb.data.entity.EntityTranscriptPage;

import java.time.OffsetDateTime;
import java.util.List;

public class DtoTranscript {
    private String fileId;
    private String name;
    private OffsetDateTime transcripted_at;
    private OffsetDateTime documented_at;
    //private String transcript;
    //private long transcriptTook;
    private String aiModel;
    //private int tokensPrompt;
    //private int tokensResponse;
    private int pageCount;
    private int version;
    private List<DtoTranscriptPage> pages;
    //private List<URL> pageImages;
    //private String transcriptHtml;
    private String title;

    public static DtoTranscript fromEntities(EntityTranscript transcript, List<EntityTranscriptPage> pages) {
        DtoTranscript dto = new DtoTranscript();
        dto.fileId = transcript.getFileId();
        dto.name = transcript.getName();
        dto.title = transcript.getName();
        dto.transcripted_at = transcript.getTranscripted_at();
        dto.documented_at = transcript.getDocumented_at();
        dto.aiModel = transcript.getAiModel();
        dto.pageCount = transcript.getPageCount();
        dto.version = transcript.getVersion();

        dto.setPages(pages.stream()
                .map(DtoTranscriptPage::fromEntity)
                .toList());

//        dto.transcript = transcript.getTranscript();
//        dto.transcriptHtml = transcript.getTranscript();
//        dto.transcriptTook = transcript.getTranscriptTook();
//        dto.tokensPrompt = transcript.getTokensPrompt();
//        dto.tokensResponse = transcript.getTokensResponse();
        return dto;
    }

//    public List<URL> getPageImages() {
//        return pageImages;
//    }
//
//    public DtoTranscript setPageImages(List<URL> pageImages) {
//        this.pageImages = pageImages;
//        return this;
//    }

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


    public String getAiModel() {
        return aiModel;
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

    public List<DtoTranscriptPage> getPages() {
        return pages;
    }

    public DtoTranscript setPages(List<DtoTranscriptPage> pages) {
        this.pages = pages;
        return this;
    }
}
