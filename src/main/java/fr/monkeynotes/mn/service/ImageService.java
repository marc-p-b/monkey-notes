package fr.monkeynotes.mn.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public interface ImageService {
    void efficientStreamImage(String username, String fileId, int imageNum, OutputStream outputStream) throws IOException;
    void efficientStreamImage(String username, String fileId, int imageNum, OutputStream outputStream, boolean temp) throws IOException;
    double compareImages(BufferedImage image1, BufferedImage image2);

    String imageAsBase64(String username, String fileId, int imageNum);
    String imageAsBase64(URL imageURL);
}
