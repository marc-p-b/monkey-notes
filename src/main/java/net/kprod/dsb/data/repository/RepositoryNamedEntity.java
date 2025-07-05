package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityNamedEntity;
import net.kprod.dsb.data.entity.IdTranscriptPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryNamedEntity extends JpaRepository<EntityNamedEntity, IdTranscriptPage> {

}
