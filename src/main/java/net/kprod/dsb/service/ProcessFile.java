package net.kprod.dsb.service;

import java.io.File;
import java.nio.file.Path;

public interface ProcessFile {
    void asyncProcessFile(String fileId, Path workingDir, File file);
}
