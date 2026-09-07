package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityTranscriptPage;
import fr.monkeynotes.mn.data.entity.IdTranscriptPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RepositoryTranscriptPage extends JpaRepository<EntityTranscriptPage, IdTranscriptPage> {
    List<EntityTranscriptPage> findByIdTranscriptPage_Username(String username);
    //TODO add username
    List<EntityTranscriptPage> findByIdTranscriptPage_FileId(String FileId);
    /**
     * The user-scoped form of the finder above. Not interchangeable with it: a MonkeySync fileId is
     * a hash of the file's virtual path alone (MonkeySyncService.createMonkeySyncId), with no
     * username in the input, so two users with the same tablet folder layout hold the same fileId
     * under different composite keys — the unscoped finder returns both of their pages.
     */
    List<EntityTranscriptPage> findByIdTranscriptPage_UsernameAndIdTranscriptPage_FileId(String username, String fileId);
    //TODO add username parameter to delete
    void deleteByIdTranscriptPage_FileId(String fileId);

    @Modifying
    @Transactional
    @Query("DELETE FROM transcript_page p where p.idTranscriptPage.username = :username")
    void deleteAllByIdTranscriptPage_Username(@Param("username") String username);
}
