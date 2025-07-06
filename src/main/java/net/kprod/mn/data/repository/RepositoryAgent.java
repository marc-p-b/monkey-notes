package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityAgent;
import net.kprod.mn.data.entity.IdFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryAgent extends JpaRepository<EntityAgent, IdFile> {
}
