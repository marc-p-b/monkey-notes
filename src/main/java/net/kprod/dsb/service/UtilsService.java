package net.kprod.dsb.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public interface UtilsService {
    Path downloadDir(String fileId);
    Path transcriptdDir(String fileId);
    Path imageDir(String fileId);
    Path tempImageDir(String fileId);
    Path tempImagePath(String fileId, int imageNumber);
    Path imagePath(String fileId, int imageNumber);
    Path imagePath(String username, String fileId, int imageNumber);
    Path tempImagePath(String username, String fileId, int imageNumber);
    URL imageURL(String username, String fileId, int imageNumber) throws MalformedURLException;
    URL tempImageURL(String username, String fileId, int imageNumber) throws MalformedURLException;
    //Path fileWorkingDir(UtilsServiceImpl.WorkingDir dirType, String fileId);
    void efficientStreamFile(File file, OutputStream outputStream) throws IOException;
    Path getUserDataPath();
    Path getUserImagesPath();
    Path getUserDownloadsPath();
    Path getUserTranscriptsPath();
    String getLocalFileName(String fileId);
}
