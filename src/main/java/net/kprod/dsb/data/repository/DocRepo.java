package net.kprod.dsb.data.repository;

import net.kprod.dsb.data.entity.Doc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocRepo extends JpaRepository<Doc, String> {

}
