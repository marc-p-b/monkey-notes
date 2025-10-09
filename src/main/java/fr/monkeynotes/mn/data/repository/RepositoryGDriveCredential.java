package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityGDriveCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryGDriveCredential extends JpaRepository<EntityGDriveCredential, String> {
}