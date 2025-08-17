package net.kprod.mn.data.dto;

import net.kprod.mn.data.entity.EntityNamedEntity;
import net.kprod.mn.data.entity.IdNamedEntity;
import net.kprod.mn.data.enums.NamedEntityVerb;

import java.util.UUID;

public class DtoNamedEntity {
    private UUID uuid;
    private NamedEntityVerb verb;
    private String value;
    private int start;
    private int end;

    public static DtoNamedEntity fromEntity(EntityNamedEntity entity) {
        return new DtoNamedEntity(entity.getVerb(), entity.getValue(), entity.getStartIndex(), entity.getEndIndex())
                .setUuid(entity.getIdNamedEntity().getUuid());
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

    private DtoNamedEntity setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public UUID getUuid() {
        return uuid;
    }

    public NamedEntityVerb getVerb() {
            return verb;
        }

    public String getValue() {
        return value;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
            return end;
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