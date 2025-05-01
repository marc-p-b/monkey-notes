package net.kprod.dsb.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ImageService {
    byte[] imageBytes(String fileId, int imageNum);
//    InputStream inputStreamImage(File file) throws IOException;
    void streamImage(File file, OutputStream outputStream) throws IOException;
    void efficientStreamImage(File file, OutputStream outputStream) throws IOException;
}
