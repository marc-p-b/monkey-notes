package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class IdTranscriptPageDiff {
    private String fileId;
    private String username;
    private int pageNumber;
    private int version;

    public IdTranscriptPageDiff() {
    }

    public static IdTranscriptPageDiff fromIdTranscriptPage(IdTranscriptPage idPage, int version) {
        IdTranscriptPageDiff idTranscriptPageDiff = new IdTranscriptPageDiff();
        idTranscriptPageDiff.username = idPage.getUsername();
        idTranscriptPageDiff.fileId = idPage.getFileId();
        idTranscriptPageDiff.pageNumber = idPage.getPageNumber();
        idTranscriptPageDiff.version = version;
        return idTranscriptPageDiff;
    }

    public static IdTranscriptPageDiff createIdTranscriptPageDiff(String username, String fileId, int pageNumber, int version) {
        IdTranscriptPageDiff idTranscriptPageDiff = new IdTranscriptPageDiff();
        idTranscriptPageDiff.username = username;
        idTranscriptPageDiff.fileId = fileId;
        idTranscriptPageDiff.pageNumber = pageNumber;
        idTranscriptPageDiff.version = version;
        return idTranscriptPageDiff;
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

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdTranscriptPageDiff that = (IdTranscriptPageDiff) o;
        return pageNumber == that.pageNumber && version == that.version && Objects.equals(fileId, that.fileId) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, username, pageNumber, version);
    }
}
