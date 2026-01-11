package fr.monkeynotes.mn.data;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

enum FileEventType {
    created,
    modified,
    deleted,
}

public class InboundFileEvent {
    private String path;
    private String fileName;
    private FileEventType eventType;
    private String timestamp;
    private int fileSize;

    public String getPath() {
        return path;
    }

    public InboundFileEvent setPath(String path) {
        this.path = path;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public InboundFileEvent setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public FileEventType getEventType() {
        return eventType;
    }

    public InboundFileEvent setEventType(FileEventType eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public InboundFileEvent setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getFileSize() {
        return fileSize;
    }

    public InboundFileEvent setFileSize(int fileSize) {
        this.fileSize = fileSize;
        return this;
    }
}
