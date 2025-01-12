package net.kprod.dsb.service;

import java.io.IOException;
import java.nio.file.Path;

public interface DriveService {
    void list() throws IOException;
    void watch() throws IOException;
    void watchStop() throws IOException;
    String getFileName(String fileId) throws IOException;
    void downloadFile(String fileId, Path destinationPath) throws IOException;
    void getChanges();
    void flushChanges();

}
