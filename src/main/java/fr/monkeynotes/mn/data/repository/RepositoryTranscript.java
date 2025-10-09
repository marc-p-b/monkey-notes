package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityTranscript;
import fr.monkeynotes.mn.data.entity.IdFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RepositoryTranscript extends JpaRepository<EntityTranscript, IdFile> {
    List<EntityTranscript> findAllByIdFileIn(Set<IdFile> idFiles);
    List<EntityTranscript> findAllByIdFile_Username(String username);

    @Query("SELECT t FROM transcript t where t.idFile.username = :username ORDER BY t.transcripted_at DESC")
    List<EntityTranscript> findRecentByIdFile_Username(@Param("username") String username, Pageable pageable);
}
