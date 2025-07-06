package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityGDriveCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryGDriveCredential extends JpaRepository<EntityGDriveCredential, String> {
}