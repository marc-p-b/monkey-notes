package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepoFolder extends JpaRepository<Folder, String> {
    Optional<Folder> findByName(String name);
    List<Folder> findAllByParentFolderId(String parentFolderId);
}
