package fr.monkeynotes.mn.service;

import com.google.api.services.drive.Drive;

import java.util.Optional;

public interface DriveService {
    void resetMap();
    Optional<String> requireAuth();
    String requiredNewAuth();
    void grantCallback(String code);
    void refreshToken();
    Drive getDrive();
    void connectCallback(Runnable callback);
    void disconnect();
}
