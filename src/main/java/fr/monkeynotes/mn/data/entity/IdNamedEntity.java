package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.UUID;

@Embeddable
public class IdNamedEntity {
    private String fileId;
    private String username;
    private int pageNumber;
    private UUID uuid;

    public IdNamedEntity() {
    }

    public static IdNamedEntity createIdNamedEntity(String username, String fileId, int pageNumber) {
        IdNamedEntity idNamedEntity = new IdNamedEntity();
        idNamedEntity.username = username;
        idNamedEntity.fileId = fileId;
        idNamedEntity.pageNumber = pageNumber;
        idNamedEntity.uuid = UUID.randomUUID();
        return idNamedEntity;
    }

    public String getFileId() {
        return fileId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUuid() {return uuid;}

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdNamedEntity that = (IdNamedEntity) o;
        return pageNumber == that.pageNumber && Objects.equals(fileId, that.fileId) && Objects.equals(username, that.username) && Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, username, pageNumber, uuid);
    }
}
