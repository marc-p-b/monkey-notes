package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityTranscript;
import net.kprod.dsb.data.entity.IdFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RepositoryTranscript extends JpaRepository<EntityTranscript, IdFile> {
    List<EntityTranscript> findAllByIdFileIn(Set<IdFile> idFiles);
}
