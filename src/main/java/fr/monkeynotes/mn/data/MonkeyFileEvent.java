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
    private String rootFolderPath;

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

    public String getRootFolderPath() {
        return rootFolderPath;
    }

    public MonkeyFileEvent setRootFolderPath(String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MonkeyFileEvent{");
        sb.append("filePath='").append(filePath).append('\'');
        sb.append(", fileName='").append(fileName).append('\'');
        sb.append(", eventType=").append(eventType);
        sb.append(", timestamp='").append(timestamp).append('\'');
        sb.append(", fileSize=").append(fileSize);
        sb.append(", content='").append(content).append('\'');
        sb.append(", rootFolderPath='").append(rootFolderPath).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
