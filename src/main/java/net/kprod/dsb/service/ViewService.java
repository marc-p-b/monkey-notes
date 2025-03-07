package net.kprod.dsb.service;

import java.io.IOException;
import java.util.List;

public interface ViewService {
    List<String> listAvailableTranscripts();
    String getTranscript(String fileId);
    List<String> listFolders();
    List<String> listTranscriptFromFolder(String folderId);
    java.io.File createTranscriptPdfFromFolder(String folderId) throws IOException;
}
