package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityAgent;
import net.kprod.dsb.data.entity.IdFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryAgent extends JpaRepository<EntityAgent, IdFile> {
}
