package net.kprod.dsb.service;

import net.kprod.dsb.data.dto.DtoFile;
import net.kprod.dsb.data.dto.FileNode;

import java.io.IOException;
import java.util.List;

public interface ViewService {
    List<String> listAvailableTranscripts();
    String getTranscript(String fileId);
    List<FileNode> listFolders();
    List<DtoFile> listTranscriptFromFolder(DtoFile folder);
    java.io.File createTranscriptPdfFromFolder(String folderId) throws IOException;
}
