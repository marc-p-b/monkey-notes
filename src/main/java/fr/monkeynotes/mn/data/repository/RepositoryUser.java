package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryUser extends JpaRepository<EntityUser, String> {
    Optional<EntityUser> findByUsernameEquals(String username);

}
