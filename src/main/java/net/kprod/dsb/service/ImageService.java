package net.kprod.dsb.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public interface ImageService {
    void efficientStreamImage(String username, String fileId, int imageNum, OutputStream outputStream) throws IOException;
    void efficientStreamImage(String username, String fileId, int imageNum, OutputStream outputStream, boolean temp) throws IOException;
    double compareImages(BufferedImage image1, BufferedImage image2);
    //String imageMd5(BufferedImage image) throws NoSuchAlgorithmException;
}
