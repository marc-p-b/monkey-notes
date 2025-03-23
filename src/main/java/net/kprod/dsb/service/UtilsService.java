package net.kprod.dsb.service;

import net.kprod.dsb.service.impl.UtilsServiceImpl;

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
}
