package fr.monkeynotes.mn.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Entity
public class EntityMonkeyFile {
    private String id;
    private String path;

    public static String sha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return "ms" + hexString;
    }

    public EntityMonkeyFile() {
    }

    public EntityMonkeyFile(String path) throws NoSuchAlgorithmException {
        this.path = path;
        this.id = sha256(path);
    }

    public String getPath() {
        return path;
    }

    public EntityMonkeyFile setPath(String path) {
        this.path = path;
        return this;
    }

    @Id
    public String getId() {
        return id;
    }

    public EntityMonkeyFile setId(String msId) {
        this.id = msId;
        return this;
    }
}
