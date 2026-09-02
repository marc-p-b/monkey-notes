package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryFile extends JpaRepository<EntityFile, IdFile> {
    Optional<EntityFile> findByIdFile_UsernameAndNameAndTypeIs(String username, String name, FileType type);
    List<EntityFile> findAllByIdFile_UsernameAndParentFolderId(String username, String parentFolderId);
    List<EntityFile> findAllByIdFile_Username(String username);
    long countByIdFile_UsernameAndType(String username, FileType type);

    /**
     * Drops one account's rows. Bulk JPQL rather than a derived delete so the statement is issued
     * as-is instead of loading every row into the persistence context first — same reasoning for
     * the deleteAllBy…_Username of every other repository the import wipes.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM file f where f.idFile.username = :username")
    void deleteAllByIdFile_Username(@Param("username") String username);
}
