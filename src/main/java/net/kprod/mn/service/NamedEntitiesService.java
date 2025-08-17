package net.kprod.mn.service;

import net.kprod.mn.data.CompletionResponse;

import java.util.List;

public interface NamedEntitiesService {
    void identifyNamedEntities(String fileId, List<CompletionResponse> listCompletionResponse);
}
