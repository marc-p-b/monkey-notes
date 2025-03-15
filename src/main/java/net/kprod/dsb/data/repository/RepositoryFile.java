package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityFile;
import net.kprod.dsb.data.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryFile extends JpaRepository<EntityFile, String> {
    Optional<EntityFile> findByNameAndTypeIs(String name, FileType type);
    List<EntityFile> findAllByParentFolderIdAndTypeIs(String parentFolderId, FileType type);
    List<EntityFile> findAllByParentFolderId(String parentFolderId);
}
