package net.kprod.dsb.data.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class IdTranscriptPage {
    private String fileId;
    private String username;
    private int pageNumber;

    public IdTranscriptPage() {
    }

    public static IdTranscriptPage createIdTranscriptPage(String username, String fileId, int pageNumber) {
        IdTranscriptPage idTranscriptPage = new IdTranscriptPage();
        idTranscriptPage.username = username;
        idTranscriptPage.fileId = fileId;
        idTranscriptPage.pageNumber = pageNumber;
        return idTranscriptPage;
    }

    public String getFileId() {
        return fileId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getUsername() {
        return username;
    }
}
