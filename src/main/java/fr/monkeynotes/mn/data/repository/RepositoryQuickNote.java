package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityQuickNote;
import fr.monkeynotes.mn.data.entity.IdQuickNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RepositoryQuickNote extends JpaRepository<EntityQuickNote, IdQuickNote> {

    /**
     * The feed order: newest first, tombstones excluded.
     */
    List<EntityQuickNote> findByIdQuickNote_UsernameAndDeletedAtIsNullOrderByCreatedAtDesc(String username);

    /**
     * Every live note, all users — for a full Lucene rebuild, mirroring repositoryTranscript.findAll().
     */
    List<EntityQuickNote> findByDeletedAtIsNull();

    /**
     * Resolves a batch of search hits back to live notes owned by this user. Scoping by username here
     * is what keeps one user's notes out of another's results, since the Lucene index itself holds no
     * username; filtering tombstones makes a stale index entry self-healing.
     */
    List<EntityQuickNote> findByIdQuickNote_UsernameAndIdQuickNote_UuidInAndDeletedAtIsNull(
            String username, Collection<UUID> uuids);
}
