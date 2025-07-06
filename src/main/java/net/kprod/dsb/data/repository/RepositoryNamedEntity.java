package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.EntityNamedEntity;
import net.kprod.dsb.data.entity.EntityTranscript;
import net.kprod.dsb.data.entity.IdNamedEntity;
import net.kprod.dsb.data.entity.IdTranscriptPage;
import org.springframework.data.domain.Pageable;
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
    void deleteByIdNamedEntity(@Param("username") String username, @Param("fileId") String fileId, @Param("pageNumber") int pageNumber);
}
