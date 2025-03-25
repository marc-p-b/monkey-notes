package net.kprod.dsb.data.dto;

import net.kprod.dsb.data.entity.EntityFile;
import net.kprod.dsb.data.enums.FileType;

import java.time.OffsetDateTime;

public class DtoFile {
    private String fileId;
    private String parentFolderId;
    private String name;
    private String md5;
    private OffsetDateTime discovered_at;
    private FileType type;

//    private int offset;

    private DtoFile() {
    }

//    public int getOffset() {
//        return offset;
//    }
//
//    public DtoFile setOffset(int offset) {
//        this.offset = offset;
//        return this;
//    }

    public String getFileId() {
        return fileId;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public String getName() {
        return name;
    }

    public String getMd5() {
        return md5;
    }

    public OffsetDateTime getDiscovered_at() {
        return discovered_at;
    }

    public FileType getType() {
        return type;
    }


    //TODO private setters
    public DtoFile setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public DtoFile setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
        return this;
    }

    public DtoFile setName(String name) {
        this.name = name;
        return this;
    }

    public DtoFile setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public DtoFile setDiscovered_at(OffsetDateTime discovered_at) {
        this.discovered_at = discovered_at;
        return this;
    }

    public DtoFile setType(FileType type) {
        this.type = type;
        return this;
    }

    public static class Builder {
        private String fileId;
        private String name;
        private String md5;

        public DtoFile pdf() {
            return new DtoFile()
                .setFileId(fileId)
                .setName(name)
                .setMd5(md5)
                .setDiscovered_at(OffsetDateTime.now())
                .setType(FileType.transcript);
        }

        public Builder setFileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setMd5(String md5) {
            this.md5 = md5;
            return this;
        }
    }

    public boolean isFolder() {
        return type == FileType.folder;
    }

    public boolean isPdf() {
        return type == FileType.transcript;
    }

    public static EntityFile toEntity(DtoFile dtoFile) {
        return new EntityFile()
                .setFileId(dtoFile.fileId)
                .setParentFolderId(dtoFile.parentFolderId)
                .setName(dtoFile.name)
                .setMd5(dtoFile.md5)
                .setDiscovered_at(dtoFile.discovered_at)
                .setType(dtoFile.type);
    }

    public static DtoFile fromEntity(EntityFile entityFile) {
        return new DtoFile()
                .setFileId(entityFile.getFileId())
                .setParentFolderId(entityFile.getParentFolderId())
                .setName(entityFile.getName())
                .setMd5(entityFile.getMd5())
                .setDiscovered_at(entityFile.getDiscovered_at())
                .setType(entityFile.getType());
    }

    @Override
    public String toString() {
        return "DtoFile{" +
                "name='" + name + '\'' +
                '}';
    }
}
