package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;

public interface EditService {
    void edit(String fileId, int pageNumber, String content);
    DtoTranscriptPage applyPatch(DtoTranscriptPage page);
}
