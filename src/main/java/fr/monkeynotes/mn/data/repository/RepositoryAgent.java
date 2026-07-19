package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryAgent extends JpaRepository<EntityAgent, String> {
    List<EntityAgent> findAllByUsername(String username);
    Optional<EntityAgent> findEntityAgentByThreadId(String threadId);
}
