package net.kprod.dsb.service;

import net.kprod.dsb.data.dto.DtoTranscript;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public interface PdfService {
    List<URL> pdf2Images(String fileId, java.io.File sourceFile, Path targetDir);
    java.io.File createTranscriptPdf(String fileId, List<DtoTranscript> listDtoTranscript) throws IOException;
}
