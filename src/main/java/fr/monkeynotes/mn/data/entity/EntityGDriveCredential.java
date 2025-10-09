package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity(name="gdrive_credential")
public class EntityGDriveCredential {

    @Id
    private String id;

    @Lob
    private byte[] credentialData;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getCredentialData() {
        return credentialData;
    }

    public void setCredentialData(byte[] credentialData) {
        this.credentialData = credentialData;
    }
}