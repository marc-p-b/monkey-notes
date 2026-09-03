package fr.monkeynotes.mn.data.entity;

import fr.monkeynotes.mn.data.enums.LogOperation;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
public class EntityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private OffsetDateTime timestamp;

    private String username;

    @Enumerated(EnumType.STRING)
    private LogOperation operation;

    private String message;

    private String monitoringId;

    public long getId() {
        return id;
    }

    public EntityLog setId(long id) {
        this.id = id;
        return this;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public EntityLog setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public EntityLog setUsername(String username) {
        this.username = username;
        return this;
    }

    public LogOperation getOperation() {
        return operation;
    }

    public EntityLog setOperation(LogOperation operation) {
        this.operation = operation;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public EntityLog setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getMonitoringId() {
        return monitoringId;
    }

    public EntityLog setMonitoringId(String monitoringId) {
        this.monitoringId = monitoringId;
        return this;
    }
}
