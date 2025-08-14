package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityNamedEntity;
import net.kprod.mn.data.entity.IdNamedEntity;
import net.kprod.mn.data.dto.DtoNamedEntity;
import net.kprod.mn.transcript.NamedEntityVerb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RepositoryNamedEntity extends JpaRepository<EntityNamedEntity, IdNamedEntity> {
    @Modifying
    @Transactional
    @Query("DELETE FROM named_entity n where n.idNamedEntity.username = :username and n.idNamedEntity.fileId = :fileId and n.idNamedEntity.pageNumber = :pageNumber")
    void delete(@Param("username") String username, @Param("fileId") String fileId, @Param("pageNumber") int pageNumber);

    @Query("SELECT n FROM named_entity n where n.idNamedEntity.username = :username and n.idNamedEntity.fileId = :fileId and n.idNamedEntity.pageNumber = :pageNumber")
    List<EntityNamedEntity> findBy(@Param("username") String username, @Param("fileId") String fileId, @Param("pageNumber") int pageNumber);

    @Query("SELECT n FROM named_entity n where n.idNamedEntity.username = :username and n.verb = :verb")
    List<EntityNamedEntity> findByVerb(@Param("username") String username, @Param("verb")NamedEntityVerb verb);
}

