package net.kprod.dsb.data.dto;

public class DtoProcess {
    String id;
    String name;
    String status;
    String description;
    String duration;

    public DtoProcess(String processId, String processName) {
        this.id = processId;
        this.name = processName;
    }

    public String getStatus() {
        return status;
    }

    public DtoProcess setStatus(String status) {
        this.status = status;
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
}
