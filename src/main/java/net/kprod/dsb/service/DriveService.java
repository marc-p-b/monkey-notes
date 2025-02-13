package net.kprod.dsb.service;

import com.google.api.services.drive.Drive;

public interface DriveService {
    //void initAuth();
    void grantCallback(String code);
    void refreshToken();
    Drive getDrive();
}
