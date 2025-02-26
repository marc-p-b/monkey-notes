package net.kprod.dsb.data;

import com.google.api.services.drive.model.File;
import net.kprod.dsb.data.entity.Doc;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class File2Process {
    private String fileId;
    private Path filePath;

    private String fileName;
    private String md5;
    //private String driveFullFolderPath;
    private String parentFolderId;
    private String parentFolderName;
    private String mimeType;
    private OffsetDateTime documentDate;


//    public File2Process(String fileId) {//, Path file) {
//        this.fileId = fileId;
//        //this.filePath = file;
//    }


    public File2Process(File file) {
        if(file == null) {
            return;
        }
        this.fileId = file.getId() != null ? file.getId() : "unknown";
        this.fileName = file.getName() != null ? file.getName() : "unknown";
        this.md5 = file.getMd5Checksum() != null ? file.getMd5Checksum() : "unknown";
        this.mimeType = file.getMd5Checksum() != null ? file.getMimeType() : "unknown";

        Pattern datePattern1 = Pattern.compile("(\\d{6})");
        Matcher m1 = datePattern1.matcher(this.fileName);

        if (m1.find()) {
            try {
                documentDate = OffsetDateTime.parse(m1.group(1), DateTimeFormatter.ofPattern("yyMMdd"));
            } catch (DateTimeParseException e) {
                //
            }
        }

    }

    public Doc asDoc() {

        Doc doc = new Doc()
                .setFileId(fileId)
                .setFileName(fileName)
                .setMd5(md5)
                .setParentFolderId(parentFolderId)
                .setParentFolderName(parentFolderName)
                .setDocumented_at(documentDate);

        return doc;
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