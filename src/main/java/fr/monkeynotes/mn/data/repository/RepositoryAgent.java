package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityAgent;
import fr.monkeynotes.mn.data.entity.IdFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryAgent extends JpaRepository<EntityAgent, IdFile> {
}
