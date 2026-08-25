package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.DtoQuickNote;
import fr.monkeynotes.mn.data.dto.DtoSearchResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SearchService {
    void initLucene();
    Map<String, List<DtoSearchResult>> search(String query);

    /**
     * Add or replace a single quicknote in the index. A no-op when indexing is disabled, so the
     * write path of QuickNoteService never depends on Lucene being switched on.
     */
    void indexQuickNote(DtoQuickNote dtoQuickNote);

    void removeQuickNote(UUID uuid);
}
