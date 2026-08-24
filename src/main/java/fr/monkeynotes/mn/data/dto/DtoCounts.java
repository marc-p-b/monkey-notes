package fr.monkeynotes.mn.data.dto;

public class DtoCounts {
    private long folders;
    private long transcripts;

    public long getFolders() {
        return folders;
    }

    public DtoCounts setFolders(long folders) {
        this.folders = folders;
        return this;
    }

    public long getTranscripts() {
        return transcripts;
    }

    public DtoCounts setTranscripts(long transcripts) {
        this.transcripts = transcripts;
        return this;
    }

    @Override
    public String toString() {
        return "DtoCounts{" +
                "folders=" + folders +
                ", transcripts=" + transcripts +
                '}';
    }
}
