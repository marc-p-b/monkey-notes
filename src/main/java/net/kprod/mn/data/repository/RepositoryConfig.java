package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityPreferences;
import net.kprod.mn.data.entity.EntityPreferencesId;
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
