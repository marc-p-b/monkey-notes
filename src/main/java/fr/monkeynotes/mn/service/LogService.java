package fr.monkeynotes.mn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.monkeynotes.mn.data.entity.EntityLog;
import fr.monkeynotes.mn.data.enums.LogOperation;
import fr.monkeynotes.mn.data.repository.RepositoryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class LogService {
    private Logger LOG = LoggerFactory.getLogger(LogService.class);

    @Autowired
    private RepositoryLog repositoryLog;

    @Autowired
    private AuthService authService;

    public void log(LogOperation operation, Map<String, String> message) {

        ObjectMapper mapper = new ObjectMapper();
        String jsonValue = "{}";

        try {
            jsonValue = mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert message items to json", e);
        }

        EntityLog entityLog = new EntityLog()
            .setOperation(operation)
            .setMessage(jsonValue)
            .setTimestamp(OffsetDateTime.now())
            .setUsername(authService.getUsernameFromContext());

        repositoryLog.save(entityLog);
    }

    public void log(LogOperation operation) {
        log(operation, new HashMap());
    }
}
