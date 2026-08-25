package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.UUID;

@Embeddable
public class IdQuickNote {
    private String username;
    private UUID uuid;

    public IdQuickNote() {
    }

    /**
     * The uuid is always supplied by the caller — MonkeySyncApp generates it on the device before
     * the note is ever pushed, which is what makes the create call idempotent across sync retries.
     */
    public static IdQuickNote createIdQuickNote(String username, UUID uuid) {
        IdQuickNote idQuickNote = new IdQuickNote();
        idQuickNote.username = username;
        idQuickNote.uuid = uuid;
        return idQuickNote;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "IdQuickNote{" +
                "username='" + username + '\'' +
                ", uuid=" + uuid +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdQuickNote that = (IdQuickNote) o;
        return Objects.equals(username, that.username) && Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, uuid);
    }
}
