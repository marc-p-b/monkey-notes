package net.kprod.dsb.service;

import com.google.api.services.drive.model.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface DriveService {
    void list() throws IOException;
    void watch() throws IOException;
    void watchStop() throws IOException;
    String getFileName(String fileId) throws IOException;
    void downloadFile(String fileId, Path destinationPath) throws IOException;
    void getChanges(String channelId);
    void flushChanges();
    File upload (String name, java.io.File file);
}
