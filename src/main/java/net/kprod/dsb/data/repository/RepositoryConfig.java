package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityConfig;
import net.kprod.dsb.data.entity.EntityConfigId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryConfig extends JpaRepository<EntityConfig, EntityConfigId> {
    Optional<EntityConfig> findByConfigId(EntityConfigId configId);
}
