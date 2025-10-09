package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityPreferences;
import fr.monkeynotes.mn.data.entity.EntityPreferencesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryConfig extends JpaRepository<EntityPreferences, EntityPreferencesId> {
    Optional<EntityPreferences> findByConfigId(EntityPreferencesId configId);
    List<EntityPreferences> findAllByConfigId_Username(String username);
    void deleteByConfigId_Username(String username);
}
