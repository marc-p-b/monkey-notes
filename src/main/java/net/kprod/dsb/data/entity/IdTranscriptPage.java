package net.kprod.dsb.data.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class IdTranscriptPage {
    private String fileId;
    private int pageNumber;

    public IdTranscriptPage() {
    }

    public IdTranscriptPage(String fileId, int pageNumber) {
        this.fileId = fileId;
        this.pageNumber = pageNumber;
    }

    public String getFileId() {
        return fileId;
    }

    public int getPageNumber() {
        return pageNumber;
    }
}
