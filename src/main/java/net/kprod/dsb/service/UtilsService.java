package net.kprod.dsb.service;

import net.kprod.dsb.data.File2Process;
import net.kprod.dsb.data.entity.IdFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface UtilsService {
    Path downloadDir(String fileId);
    Path transcriptdDir(String fileId);
    Path imageDir(String fileId);
    Path imagePath(String fileId, int imageNumber);
    Path imagePath(String username, String fileId, int imageNumber);
    URL imageURL(String username, String fileId, int imageNumber) throws MalformedURLException;
    //Path fileWorkingDir(UtilsServiceImpl.WorkingDir dirType, String fileId);
    void efficientStreamFile(File file, OutputStream outputStream) throws IOException;
    Path getUserDataPath();
    Path getUserImagesPath();
    Path getUserDownloadsPath();
    Path getUserTranscriptsPath();
    String getLocalFileName(String fileId);
    OffsetDateTime identifyDates(File2Process f2p);
    IdFile idFile(String fileId);
}
