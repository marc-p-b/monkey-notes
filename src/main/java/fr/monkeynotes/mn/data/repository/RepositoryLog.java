package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryLog extends JpaRepository<EntityLog, Long> {
}
