package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryTranscript extends JpaRepository<EntityTranscript, String> {

}
