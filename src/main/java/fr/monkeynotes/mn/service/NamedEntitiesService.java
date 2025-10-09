package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.CompletionResponse;

import java.util.List;

public interface NamedEntitiesService {
    void identifyNamedEntities(String fileId, List<CompletionResponse> listCompletionResponse);
}
