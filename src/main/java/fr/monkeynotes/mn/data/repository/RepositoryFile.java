package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryFile extends JpaRepository<EntityFile, IdFile> {
    Optional<EntityFile> findByNameAndTypeIs(String name, FileType type);
    List<EntityFile> findAllByParentFolderId(String parentFolderId);
    List<EntityFile> findAllByIdFile_Username(String username);
}
