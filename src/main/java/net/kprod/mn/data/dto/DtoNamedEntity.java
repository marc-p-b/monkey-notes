package net.kprod.mn.data.dto;

import net.kprod.mn.data.entity.EntityNamedEntity;
import net.kprod.mn.data.entity.IdNamedEntity;
import net.kprod.mn.data.enums.NamedEntityVerb;

import java.util.UUID;

public class DtoNamedEntity {
    private UUID uuid;
    private NamedEntityVerb verb;
    private String value;
    private String fileId;
    private String fileName;
    private int pageNumber;
    private int start;
    private int end;

    public static DtoNamedEntity fromEntity(EntityNamedEntity entity) {
        return new DtoNamedEntity(entity.getVerb(), entity.getValue(), entity.getStartIndex(), entity.getEndIndex())
                .setUuid(entity.getIdNamedEntity().getUuid())
                .setFileId(entity.getIdNamedEntity().getFileId())
                .setPageNumber(entity.getIdNamedEntity().getPageNumber());
    }

    public EntityNamedEntity toEntity(String username, String fileId, int pageNumber) {
        IdNamedEntity idNamedEntity = IdNamedEntity.createIdNamedEntity(username, fileId, pageNumber);
        return new EntityNamedEntity()
                .setIdNamedEntity(idNamedEntity)
                .setVerb(this.getVerb())
                .setValue(this.getValue())
                .setStartIndex(this.getStart())
                .setEndIndex(this.getEnd());
    }

    public DtoNamedEntity(NamedEntityVerb verb, String value, Integer start, Integer end) {
        this.verb = verb;
        this.value = value;
        this.start = start;
        this.end = end;
    }

    public UUID getUuid() {
        return uuid;
    }

    public DtoNamedEntity setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public NamedEntityVerb getVerb() {
        return verb;
    }

    public DtoNamedEntity setVerb(NamedEntityVerb verb) {
        this.verb = verb;
        return this;
    }

    public String getValue() {
        return value;
    }

    public DtoNamedEntity setValue(String value) {
        this.value = value;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public DtoNamedEntity setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public DtoNamedEntity setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public DtoNamedEntity setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public int getStart() {
        return start;
    }

    public DtoNamedEntity setStart(int start) {
        this.start = start;
        return this;
    }

    public int getEnd() {
        return end;
    }

    public DtoNamedEntity setEnd(int end) {
        this.end = end;
        return this;
    }

    @Override
    public String toString() {
        return "TranscriptCommand{" +
                "verb=" + verb +
                ", value='" + value + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}