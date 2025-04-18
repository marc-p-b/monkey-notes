package net.kprod.dsb.data.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class IdFile {
    private String fileId;
    private String username;

    public IdFile() {
    }

    public static IdFile createIdFile(String username, String fileId) {
        IdFile idFile = new IdFile();
        idFile.fileId = fileId;
        idFile.username = username;
        return idFile;
    }

    public String getFileId() {
        return fileId;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "IdFile{" +
                "fileId='" + fileId + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
