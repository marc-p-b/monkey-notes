package net.kprod.dsb.service;

import java.io.IOException;
import java.io.OutputStream;

public interface ImageService {
    void efficientStreamImage(String fileId, int imageNum, OutputStream outputStream) throws IOException;
}
