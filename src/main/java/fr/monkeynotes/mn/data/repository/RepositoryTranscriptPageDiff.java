package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityTranscriptPageDiff;
import fr.monkeynotes.mn.data.entity.IdTranscriptPageDiff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RepositoryTranscriptPageDiff extends JpaRepository<EntityTranscriptPageDiff, IdTranscriptPageDiff> {
    List<EntityTranscriptPageDiff> findAllByIdTranscriptPageDiff_Username(String username);
    List<EntityTranscriptPageDiff> findAllByIdTranscriptPageDiff_FileId(String FileId);
    //TODO add username parameter to delete
    void deleteByIdTranscriptPageDiff_FileId(String fileId);

    @Modifying
    @Transactional
    @Query("DELETE FROM transcript_page_diff d where d.idTranscriptPageDiff.username = :username")
    void deleteAllByIdTranscriptPageDiff_Username(@Param("username") String username);
}
