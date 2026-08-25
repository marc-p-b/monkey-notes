package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.dto.DtoNamedEntity;
import fr.monkeynotes.mn.data.dto.DtoQuickNote;
import fr.monkeynotes.mn.data.entity.EntityQuickNote;
import fr.monkeynotes.mn.data.entity.IdQuickNote;
import fr.monkeynotes.mn.data.enums.QuickNoteSource;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntity;
import fr.monkeynotes.mn.data.repository.RepositoryQuickNote;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.NamedEntitiesService;
import fr.monkeynotes.mn.service.QuickNoteService;
import fr.monkeynotes.mn.service.SearchService;
import fr.monkeynotes.mn.utils.TranscriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuickNoteServiceImpl implements QuickNoteService {
    private Logger LOG = LoggerFactory.getLogger(QuickNoteServiceImpl.class);

    private static final int TITLE_MAX_LENGTH = 512;

    @Autowired
    private AuthService authService;

    @Autowired
    private RepositoryQuickNote repositoryQuickNote;

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    @Autowired
    private NamedEntitiesService namedEntitiesService;

    @Autowired
    private SearchService searchService;

    @Override
    @Transactional
    public DtoQuickNote save(DtoQuickNote dtoQuickNote) {
        String username = authService.getUsernameFromContext();

        //a note created from the web has no client-side uuid yet
        UUID uuid = dtoQuickNote.getUuid() != null ? dtoQuickNote.getUuid() : UUID.randomUUID();
        IdQuickNote idQuickNote = IdQuickNote.createIdQuickNote(username, uuid);

        Optional<EntityQuickNote> existing = repositoryQuickNote.findById(idQuickNote);

        EntityQuickNote entityQuickNote;
        if (existing.isPresent()) {
            entityQuickNote = existing.get();
            entityQuickNote.bumpVersion();
            //a re-push of a note the user had deleted elsewhere resurrects it, which is the only
            //sensible reading while the device is the single source of truth
            entityQuickNote.setDeletedAt(null);
            LOG.info("updating quicknote {} to version {}", uuid, entityQuickNote.getVersion());
        } else {
            entityQuickNote = new EntityQuickNote()
                    .setIdQuickNote(idQuickNote)
                    .setCreatedAt(dtoQuickNote.getCreatedAt() != null
                            ? dtoQuickNote.getCreatedAt()
                            : OffsetDateTime.now())
                    .setSource(dtoQuickNote.getSource() != null
                            ? dtoQuickNote.getSource()
                            : QuickNoteSource.web);
            LOG.info("creating quicknote {}", uuid);
        }

        entityQuickNote
                .setBody(dtoQuickNote.getBody())
                .setTitle(deriveTitle(dtoQuickNote.getBody()))
                .setUpdatedAt(OffsetDateTime.now());

        EntityQuickNote saved = repositoryQuickNote.save(entityQuickNote);

        //reuses the transcript pipeline's extraction wholesale — it already deletes the previous
        //entities for this (username, fileId, pageNumber) before re-extracting, so an edit doesn't
        //accumulate stale tags
        namedEntitiesService.saveNamedEntitiesFromContent(uuid.toString(), QUICKNOTE_PAGE_NUMBER,
                dtoQuickNote.getBody());

        DtoQuickNote result = DtoQuickNote.fromEntity(saved);
        //an upsert in the index too, so an edit replaces the note's document rather than adding one
        searchService.indexQuickNote(result);

        return result;
    }

    @Override
    @Transactional
    public boolean delete(UUID uuid) {
        String username = authService.getUsernameFromContext();
        IdQuickNote idQuickNote = IdQuickNote.createIdQuickNote(username, uuid);

        Optional<EntityQuickNote> existing = repositoryQuickNote.findById(idQuickNote);
        if (existing.isEmpty() || existing.get().getDeletedAt() != null) {
            return false;
        }

        EntityQuickNote entityQuickNote = existing.get();
        entityQuickNote.setDeletedAt(OffsetDateTime.now());
        entityQuickNote.setUpdatedAt(OffsetDateTime.now());
        entityQuickNote.bumpVersion();
        repositoryQuickNote.save(entityQuickNote);

        //the row survives as a tombstone but its tags must stop showing up in the entity views
        repositoryNamedEntity.delete(username, uuid.toString(), QUICKNOTE_PAGE_NUMBER);

        //unlike a deleted transcript, a deleted note leaves no stale index entry behind
        searchService.removeQuickNote(uuid);

        LOG.info("soft-deleted quicknote {}", uuid);
        return true;
    }

    @Override
    public List<DtoQuickNote> list() {
        String username = authService.getUsernameFromContext();

        //one query for the whole feed's entities rather than one per note, then grouped in memory —
        //the fileId of a quicknote entity is the note's uuid
        Map<String, List<DtoNamedEntity>> entitiesByNote = repositoryNamedEntity
                .findByPageNumber(username, QUICKNOTE_PAGE_NUMBER)
                .stream()
                .map(DtoNamedEntity::fromEntity)
                .collect(Collectors.groupingBy(DtoNamedEntity::getFileId));

        return repositoryQuickNote
                .findByIdQuickNote_UsernameAndDeletedAtIsNullOrderByCreatedAtDesc(username)
                .stream()
                .map(DtoQuickNote::fromEntity)
                .map(dto -> dto.setListNamedEntities(
                        sortedByStart(entitiesByNote.get(dto.getUuid().toString()))))
                .toList();
    }

    @Override
    public Optional<DtoQuickNote> get(UUID uuid) {
        String username = authService.getUsernameFromContext();
        IdQuickNote idQuickNote = IdQuickNote.createIdQuickNote(username, uuid);

        return repositoryQuickNote.findById(idQuickNote)
                .filter(e -> e.getDeletedAt() == null)
                .map(DtoQuickNote::fromEntity)
                .map(dto -> dto.setListNamedEntities(sortedByStart(
                        repositoryNamedEntity.findBy(username, uuid.toString(), QUICKNOTE_PAGE_NUMBER)
                                .stream()
                                .map(DtoNamedEntity::fromEntity)
                                .toList())));
    }

    /**
     * The frontend renderer walks entities in order and shifts later offsets by the length its own
     * replacements added, so it only produces correct output on an ascending, non-overlapping list.
     * Neither the table nor the grouping above guarantees an order, so sort here.
     */
    private List<DtoNamedEntity> sortedByStart(List<DtoNamedEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .sorted(Comparator.comparingInt(DtoNamedEntity::getStart))
                .toList();
    }

    /**
     * A leading markdown heading wins, otherwise the first non-blank line. Reuses the transcript
     * heading parser so "# Foo" means the same thing in a quicknote as it does on a scanned page.
     */
    private String deriveTitle(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        List<DtoNamedEntity> titles = TranscriptUtils.identifyTitles(body);
        String title = titles.stream()
                .map(DtoNamedEntity::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElseGet(() -> body.lines()
                        .map(String::trim)
                        .filter(l -> !l.isEmpty())
                        .findFirst()
                        .orElse(null));

        if (title == null) {
            return null;
        }
        return title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) : title;
    }
}
