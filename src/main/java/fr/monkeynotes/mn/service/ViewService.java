package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.ViewOptions;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptDetails;
import fr.monkeynotes.mn.data.dto.FileNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ViewService {
    List<FileNode> listAllNodes();
    List<FileNode> listRootLevel();
    List<FileNode> listLevel(String folderId);
    File createTranscriptPdf(String fileId) throws IOException;
    java.io.File createTranscriptPdfFromFolder(String folderId) throws IOException;
    DtoTranscript getTranscript(String fileId, ViewOptions options);// throws IOException;
    List<DtoTranscript> listTranscriptFromFolderRecurs (String folderId);
    List<DtoTranscriptDetails> listRecentTranscripts(int from, int to);
    void delete(String fileId);
    String getContent(DtoTranscript dtoTranscript) throws ServiceException;
}
