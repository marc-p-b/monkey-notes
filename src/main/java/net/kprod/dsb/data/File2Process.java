package net.kprod.dsb.data;

import java.nio.file.Path;

public class File2Process {
        private String fileId;
        private Path filePath;
        private String md5;

    public File2Process(String fileId, Path file) {
        this.fileId = fileId;
        this.filePath = file;
    }

    public String getFileId() {
        return fileId;
    }

    public Path getFilePath() {
        return filePath;
    }

    public File2Process setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public File2Process setMd5(String md5) {
        this.md5 = md5;
        return this;
    }
}