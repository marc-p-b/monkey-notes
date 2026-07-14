package fr.monkeynotes.mn.data.repository;

import fr.monkeynotes.mn.data.entity.EntityAgentMessage;
import fr.monkeynotes.mn.data.entity.IdAgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryAgentMessage extends JpaRepository<EntityAgentMessage, IdAgentMessage> {
    List<EntityAgentMessage> findByIdAgentMessage_UsernameAndIdAgentMessage_FileIdOrderByIdAgentMessage_Sequence(String username, String fileId);
}
