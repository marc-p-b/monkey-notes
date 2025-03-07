package net.kprod.dsb.service;

import net.kprod.dsb.data.entity.Doc;

import java.util.List;

public interface ViewService {
    List<String> listAvailableTranscripts();
    String getTranscript(String fileId);
    List<String> listFolders();
    List<String> listTranscriptFromFolder(String folderId);
}
