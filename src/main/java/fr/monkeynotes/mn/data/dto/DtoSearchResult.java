package fr.monkeynotes.mn.data.dto;

import java.util.Objects;

public class DtoSearchResult {
    private String id;
    private String title;
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