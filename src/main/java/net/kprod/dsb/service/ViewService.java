package net.kprod.dsb.service;

import net.kprod.dsb.data.ViewOptions;
import net.kprod.dsb.data.dto.DtoTranscript;
import net.kprod.dsb.data.dto.FileNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ViewService {
    List<String> listAvailableTranscripts();
    List<FileNode> listFolders();
    File createTranscriptPdf(String fileId) throws IOException;
    java.io.File createTranscriptPdfFromFolder(String folderId) throws IOException;
    DtoTranscript getTranscript(String fileId, ViewOptions options) throws IOException;
    List<DtoTranscript> listTranscriptFromFolderRecurs (String folderId);
    void delete(String fileId);
}
