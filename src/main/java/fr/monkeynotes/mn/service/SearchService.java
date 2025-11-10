package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.DtoSearchResult;

import java.util.List;

public interface SearchService {
    List<DtoSearchResult> search(String query);
}
