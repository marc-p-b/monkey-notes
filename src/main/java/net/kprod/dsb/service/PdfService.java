package net.kprod.dsb.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface PdfService {
    List<Path> pdf2Images(String fileId, java.io.File sourceFile, Path targetDir);
    java.io.File createTranscriptPdf(String fileId, String textContent) throws IOException;
}
