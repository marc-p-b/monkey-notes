package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Embeddable;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdTranscriptPage that = (IdTranscriptPage) o;
        return pageNumber == that.pageNumber && Objects.equals(fileId, that.fileId) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, username, pageNumber);
    }
}
