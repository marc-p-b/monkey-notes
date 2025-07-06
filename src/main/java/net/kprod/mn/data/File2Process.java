package net.kprod.mn.data;

import com.google.api.services.drive.model.File;
import net.kprod.mn.data.entity.EntityFile;
import net.kprod.mn.data.entity.IdFile;
import net.kprod.mn.data.enums.FileType;

import java.nio.file.Path;

public class File2Process {
    private String fileId;
    private Path filePath;

    private String fileName;
    private String md5;
    private String parentFolderId;
    private String parentFolderName;
    private String mimeType;



    public File2Process(File file) {
        if(file == null) {
            return;
        }
        this.fileId = file.getId() != null ? file.getId() : "unknown";
        this.fileName = file.getName() != null ? file.getName() : "unknown";
        this.md5 = file.getMd5Checksum() != null ? file.getMd5Checksum() : "unknown";
        this.mimeType = file.getMd5Checksum() != null ? file.getMimeType() : "unknown";
    }

    public EntityFile asEntity(String username) {

        return new EntityFile()
                .setType(FileType.transcript)
                .setIdFile(IdFile.createIdFile(username, fileId))
                .setName(fileName)
                .setMd5(md5)
                .setParentFolderId(parentFolderId);

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