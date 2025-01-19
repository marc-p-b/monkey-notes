package net.kprod.dsb.service;

import java.io.IOException;

public interface PdfService {
    java.io.File createTranscriptPdf(String fileId, String textContent) throws IOException;
}
