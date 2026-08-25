package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.DtoQuickNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuickNoteService {

    /**
     * Page number used when a quicknote's named entities are stored in the shared named_entity
     * table. Quicknotes have no pages, so -1 marks a row as belonging to a quicknote rather than to
     * a transcript page — which keeps one tag/person index across both kinds of content.
     */
    int QUICKNOTE_PAGE_NUMBER = -1;

    /**
     * Create or replace the note identified by the DTO's uuid. Deliberately an upsert: the companion
     * app retries every failed push on each sync tick, so a lost response must not duplicate a note.
     */
    DtoQuickNote save(DtoQuickNote dtoQuickNote);

    /**
     * Soft-delete — sets the tombstone and drops the note's named entities.
     * Returns false when the note does not exist (or is already deleted) for the current user.
     */
    boolean delete(UUID uuid);

    List<DtoQuickNote> list();

    Optional<DtoQuickNote> get(UUID uuid);
}
