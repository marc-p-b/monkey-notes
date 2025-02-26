package net.kprod.dsb.data;

import com.google.api.services.drive.model.File;

import java.nio.file.Path;

public class File2Process {
    private String fileId;
    private Path filePath;

    private String fileName;
    private String md5;
    //private String driveFullFolderPath;
    private String parentFolderId;
    private String parentFolderName;
    private String mimeType;


//    public File2Process(String fileId) {//, Path file) {
//        this.fileId = fileId;
//        //this.filePath = file;
//    }


    public File2Process(File file) {
        this.fileId = file.getId();
        this.fileName = file.getName();
        this.md5 = file.getMd5Checksum();
        this.mimeType = file.getMimeType();
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

    public File2Process setFilePath(Path filePath) {
        this.filePath = filePath;
        return this;
    }

//    public String getDriveFullFolderPath() {
//        return driveFullFolderPath;
//    }
//
//    public File2Process setDriveFullFolderPath(String driveFullFolderPath) {
//        this.driveFullFolderPath = driveFullFolderPath;
//        return this;
//    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public File2Process setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
        return this;
    }

    public String getParentFolderName() {
        return parentFolderName;
    }

    public File2Process setParentFolderName(String parentFolderName) {
        this.parentFolderName = parentFolderName;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public File2Process setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public File2Process setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }
}