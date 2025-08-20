package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityNamedEntityIndex;
import net.kprod.mn.data.entity.IdNamedEntityIndex;
import net.kprod.mn.data.enums.NamedEntityVerb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryNamedEntityIndex extends JpaRepository<EntityNamedEntityIndex, IdNamedEntityIndex> {
    @Query("SELECT n FROM named_entity_index n where n.idNamedEntityIndex.username = :username and n.idNamedEntityIndex.verb = :verb")
    List<EntityNamedEntityIndex> findByVerb(@Param("username") String username, @Param("verb") NamedEntityVerb verb);
}

