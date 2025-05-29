package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityGDriveCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryGDriveCredential extends JpaRepository<EntityGDriveCredential, String> {
}