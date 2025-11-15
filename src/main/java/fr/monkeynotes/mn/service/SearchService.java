package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.DtoSearchResult;

import java.util.List;
import java.util.Map;

public interface SearchService {
    void initLucene();
    Map<String, List<DtoSearchResult>> search(String query);
}
