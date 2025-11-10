package fr.monkeynotes.mn.data.dto;

public class DtoSearchResult {
    private String id;
    private String title;

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
}