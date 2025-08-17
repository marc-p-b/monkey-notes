package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityNamedEntity;
import net.kprod.mn.data.entity.EntityNamedEntityIndex;
import net.kprod.mn.data.entity.IdNamedEntityIndex;
import net.kprod.mn.data.entity.IdNamedEntity;
import net.kprod.mn.data.enums.NamedEntityVerb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RepositoryNamedEntityIndex extends JpaRepository<EntityNamedEntityIndex, IdNamedEntityIndex> {
}

