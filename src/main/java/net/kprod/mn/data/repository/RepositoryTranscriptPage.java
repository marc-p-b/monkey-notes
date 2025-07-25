package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityTranscriptPage;
import net.kprod.mn.data.entity.IdTranscriptPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryTranscriptPage extends JpaRepository<EntityTranscriptPage, IdTranscriptPage> {
    List<EntityTranscriptPage> findByIdTranscriptPage_Username(String username);
    List<EntityTranscriptPage> findByIdTranscriptPage_FileId(String FileId);
    //TODO add username parameter to delete
    void deleteByIdTranscriptPage_FileId(String fileId);
}
