package net.kprod.mn.data.dto;

import net.kprod.mn.data.entity.EntityPreferences;
import net.kprod.mn.data.entity.EntityFile;
import net.kprod.mn.data.entity.EntityTranscript;
import net.kprod.mn.data.entity.EntityTranscriptPage;

import java.util.List;

public class DtoExport {
    private List<EntityFile> files;
    private List<EntityTranscript> transcripts;
    private List<EntityTranscriptPage> pages;
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

    public List<EntityPreferences> getPreferences() {
        return preferences;
    }

    public DtoExport setPreferences(List<EntityPreferences> preferences) {
        this.preferences = preferences;
        return this;
    }
}
