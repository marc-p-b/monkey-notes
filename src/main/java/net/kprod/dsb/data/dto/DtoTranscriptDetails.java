package net.kprod.dsb.data.dto;

public class DtoTranscriptDetails {
    private DtoTranscript transcript;
    private DtoFile parent;

    public DtoTranscriptDetails(DtoTranscript transcript, DtoFile parentDtoFile) {
        this.transcript = transcript;
        this.parent = parentDtoFile;
    }

    public DtoTranscript getTranscript() {
        return transcript;
    }

    public DtoFile getParent() {
        return parent;
    }
}
