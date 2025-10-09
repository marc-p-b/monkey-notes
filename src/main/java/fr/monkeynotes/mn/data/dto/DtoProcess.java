package fr.monkeynotes.mn.data.dto;

public class DtoProcess {
    private String id;
    private String name;
    private String statusStr;
    private Status status;
    private String description;
    private String duration;

    public enum Status {
        unknown,
        running,
        failed,
        completed, error;
    }

    public DtoProcess(String processId, String processName) {
        this.id = processId;
        this.name = processName;
    }

    public String getStatusStr() {
        return statusStr;
    }

    public DtoProcess setStatusStr(String statusStr) {
        this.statusStr = statusStr;
        return this;
    }

    public String getId() {
        return id;
    }

    public DtoProcess setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DtoProcess setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public DtoProcess setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDuration() {
        return duration;
    }

    public DtoProcess setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public DtoProcess setStatus(Status status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return "DtoProcess{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status='" + statusStr + '\'' +
                ", description='" + description + '\'' +
                ", duration='" + duration + '\'' +
                '}';
    }
}
