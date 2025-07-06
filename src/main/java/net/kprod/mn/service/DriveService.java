package net.kprod.mn.service;

import com.google.api.services.drive.Drive;

import java.util.Optional;

public interface DriveService {
    Optional<String> requireAuth();
    String requiredNewAuth();
    void grantCallback(String code);
    void refreshToken();
    Drive getDrive();
    void connectCallback(Runnable callback);
}
