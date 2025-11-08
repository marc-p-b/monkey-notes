package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.CompletionResponse;

import java.util.List;

public interface NamedEntitiesService {
    void saveNamedEntities(String fileId, List<CompletionResponse> listCompletionResponse);
    void saveNamedEntitiesFromContent(String fileId, int pageNumber, String content);
}
