package fr.monkeynotes.mn.data.dto;

import java.time.OffsetDateTime;
import java.util.Objects;

public class DtoSearchResult {

    public enum SRType {
        content,
        title
    }

    private String id;
    private SRType srType;
    private String title;
    private OffsetDateTime documented_at;
    private int pageNumber;

    public String getId() {
        return id;
    }

    public DtoSearchResult setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public DtoSearchResult setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public DtoSearchResult setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public DtoSearchResult setSrType(SRType srType) {
        this.srType = srType;
        return this;
    }

    public SRType getSrType() {
        return srType;
    }

    public OffsetDateTime getDocumented_at() {
        return documented_at;
    }

    public DtoSearchResult setDocumented_at(OffsetDateTime documented_at) {
        this.documented_at = documented_at;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DtoSearchResult that = (DtoSearchResult) o;
        return Objects.equals(id, that.id) && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }
}