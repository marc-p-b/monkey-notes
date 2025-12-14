package fr.monkeynotes.mn.data.dto;

public class AsyncProcessFileEvent {
    private String fileName;
    private String fileId;
    private String folderName;
    private int totalPages;
    private int modifiedPages;
    private boolean notified;

    public AsyncProcessFileEvent(String fileId, String fileName, String folderName) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.folderName = folderName;
    }

    public AsyncProcessFileEvent setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        return this;
    }

    public AsyncProcessFileEvent setModifiedPages(int modifiedPages) {
        this.modifiedPages = modifiedPages;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getModifiedPages() {
        return modifiedPages;
    }

    public boolean notNotified() {
        return !notified;
    }

    public void nofified() {
        notified = true;
    }
}
