package fr.monkeynotes.mn.data;

import com.google.api.services.drive.model.File;
import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.enums.FileType;

import java.nio.file.Path;

public class File2Process {

    public enum File2ProcessType {
        legacy,
        monkeySync;
    }
    private String fileId;
    private Path filePath;

    private File2ProcessType file2ProcessType;

    private String fileName;
    private String md5;
    private String parentFolderId;
    private String parentFolderName;
    private String mimeType;
    private boolean force;

    public File2Process(File file) {
        if(file == null) {
            return;
        }
        this.fileId = file.getId() != null ? file.getId() : "unknown";
        this.fileName = file.getName() != null ? file.getName() : "unknown";
        this.md5 = file.getMd5Checksum() != null ? file.getMd5Checksum() : "unknown";
        this.mimeType = file.getMd5Checksum() != null ? file.getMimeType() : "unknown";

        this.file2ProcessType = File2ProcessType.legacy;
    }

    public File2Process() {
    }

    public File2ProcessType getFile2ProcessType() {
        return file2ProcessType;
    }

    public File2Process setFile2ProcessType(File2ProcessType file2ProcessType) {
        this.file2ProcessType = file2ProcessType;
        return this;
    }

    public boolean isLegacy() {
        return file2ProcessType == File2ProcessType.legacy;
    }

    public boolean isMonkeySync() {
        return file2ProcessType == File2ProcessType.monkeySync;
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

    public boolean isForce() {
        return force;
    }

    public File2Process setForce(boolean force) {
        this.force = force;
        return this;
    }
}