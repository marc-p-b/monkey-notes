package net.kprod.mn.data.repository;

import net.kprod.mn.data.entity.EntityUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryUser extends JpaRepository<EntityUser, String> {
    Optional<EntityUser> findByUsernameEquals(String username);

}
