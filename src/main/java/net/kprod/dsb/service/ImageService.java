package net.kprod.dsb.service;

import java.nio.file.Path;
import java.util.List;

public interface ImageService {
    List<Path> pdf2Images(String fileId, java.io.File sourceFile, Path targetDir);
    byte[] getImage(String fileId, String imagename);
}
