package fr.monkeynotes.mn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.monkeynotes.mn.data.entity.EntityLog;
import fr.monkeynotes.mn.data.enums.LogOperation;
import fr.monkeynotes.mn.data.repository.RepositoryLog;
import fr.monkeynotes.mn.monitoring.MonitoringService;
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

    @Autowired
    private MonitoringService monitoringService;

    public void log(LogOperation operation, Map<String, Object> keyValues) {

        ObjectMapper mapper = new ObjectMapper();
        String jsonValue = "{}";

        try {
            jsonValue = mapper.writeValueAsString(keyValues);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert message items to json", e);
        }

        EntityLog entityLog = new EntityLog()
            .setOperation(operation)
            .setMessage(jsonValue)
            .setTimestamp(OffsetDateTime.now())
            .setUsername(authService.getUsernameFromContext())
            .setMonitoringId(monitoringService.getCurrentMonitoringData().getId());

        repositoryLog.save(entityLog);
    }

    public void log(LogOperation operation) {
        log(operation, new HashMap());
    }

    public void success(LogOperation operation) {
        log(operation, Map.of("status", "success"));
    }

    public void success(LogOperation operation, String message) {
        log(operation, Map.of("status", "success", "message", message));
    }

    public void success(LogOperation operation, String msId, String message) {
        log(operation, Map.of(
                "status", "success",
                "msId", msId,
                "message", message));
    }

    public void warn(LogOperation operation) {
        log(operation, Map.of("status", "warning"));
    }

    public void warn(LogOperation operation, String message) {
        log(operation, Map.of("status", "warning", "message", message));
    }

    public void failure(LogOperation operation) {
        log(operation, Map.of("status", "failure"));
    }

    public void failure(LogOperation operation, String message) {
        log(operation, Map.of("status", "failure", "message", message));
    }

    public void failure(LogOperation operation, String msId, String message) {
        log(operation, Map.of(
                "status", "success",
                "msId", msId,
                "message", message));
    }
}
