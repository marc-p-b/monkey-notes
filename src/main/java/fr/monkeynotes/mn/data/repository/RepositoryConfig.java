package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityPreferences;
import fr.monkeynotes.mn.data.entity.EntityPreferencesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryConfig extends JpaRepository<EntityPreferences, EntityPreferencesId> {
    Optional<EntityPreferences> findByConfigId(EntityPreferencesId configId);
    List<EntityPreferences> findAllByConfigId_Username(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM preferences p where p.configId.username = :username")
    void deleteAllByConfigId_Username(@Param("username") String username);
}
