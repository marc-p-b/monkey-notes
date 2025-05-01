package net.kprod.dsb.service;

import net.kprod.dsb.service.impl.UtilsServiceImpl;

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
    Path imagePath(String fileId, int imageNumber);
    URL imageURL(String fileId, int imageNumber) throws MalformedURLException;
    Path fileWorkingDir(UtilsServiceImpl.WorkingDir dirType, String fileId);
    void efficientStreamFile(File file, OutputStream outputStream) throws IOException;
}
