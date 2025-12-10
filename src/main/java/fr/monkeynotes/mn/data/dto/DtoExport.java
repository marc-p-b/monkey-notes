package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.entity.*;

import java.util.List;

public class DtoExport {
    private List<EntityFile> files;
    private List<EntityTranscript> transcripts;
    private List<EntityTranscriptPage> pages;
    private List<EntityTranscriptPageDiff> pageDiffs;
    private List<EntityNamedEntity> namedEntities;
    private List<EntityNamedEntityIndex> namedEntityIndexes;
    private List<EntityPreferences> preferences;

    public List<EntityFile> getFiles() {
        return files;
    }

    public DtoExport setFiles(List<EntityFile> files) {
        this.files = files;
        return this;
    }

    public List<EntityTranscript> getTranscripts() {
        return transcripts;
    }

    public DtoExport setTranscripts(List<EntityTranscript> transcripts) {
        this.transcripts = transcripts;
        return this;
    }

    public List<EntityTranscriptPage> getPages() {
        return pages;
    }

    public DtoExport setPages(List<EntityTranscriptPage> pages) {
        this.pages = pages;
        return this;
    }

    public List<EntityTranscriptPageDiff> getPageDiffs() {
        return pageDiffs;
    }

    public DtoExport setPageDiffs(List<EntityTranscriptPageDiff> pageDiffs) {
        this.pageDiffs = pageDiffs;
        return this;
    }

    public List<EntityNamedEntity> getNamedEntities() {
        return namedEntities;
    }

    public DtoExport setNamedEntities(List<EntityNamedEntity> namedEntities) {
        this.namedEntities = namedEntities;
        return this;
    }

    public List<EntityNamedEntityIndex> getNamedEntityIndexes() {
        return namedEntityIndexes;
    }

    public DtoExport setNamedEntityIndexes(List<EntityNamedEntityIndex> namedEntityIndexes) {
        this.namedEntityIndexes = namedEntityIndexes;
        return this;
    }

    public List<EntityPreferences> getPreferences() {
        return preferences;
    }

    public DtoExport setPreferences(List<EntityPreferences> preferences) {
        this.preferences = preferences;
        return this;
    }
}
