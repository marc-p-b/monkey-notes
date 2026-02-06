package fr.monkeynotes.mn.data;

enum FileEventType {
    created,
    modified,
    deleted,
}

public class MonkeyFileEvent {
    private String filePath;
    private String fileName;
    private FileEventType eventType;
    private String timestamp;
    private int fileSize;
    private String content;

    public String getFilePath() {
        return filePath;
    }

    public MonkeyFileEvent setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public MonkeyFileEvent setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public FileEventType getEventType() {
        return eventType;
    }

    public MonkeyFileEvent setEventType(FileEventType eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public MonkeyFileEvent setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getFileSize() {
        return fileSize;
    }

    public MonkeyFileEvent setFileSize(int fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public String getContent() {
        return content;
    }

    public MonkeyFileEvent setContent(String content) {
        this.content = content;
        return this;
    }
}
