package fr.monkeynotes.mn.data.dto;

import java.util.ArrayList;
import java.util.List;

public class FileNode {
    private DtoFile dtoFile;
    //null for a folder, and for a transcript row whose transcript is missing from the database
    private DtoTranscriptDetails transcriptDetails;
    private List<FileNode> children;
    private String name;
    private boolean folder;

    public FileNode(DtoFile dtoFile) {
        children = new ArrayList<>();
        this.dtoFile = dtoFile;
        this.name = dtoFile.getName();
        this.folder = dtoFile.isFolder();
    }

    public DtoTranscriptDetails getTranscriptDetails() {
        return transcriptDetails;
    }

    public FileNode setTranscriptDetails(DtoTranscriptDetails transcriptDetails) {
        this.transcriptDetails = transcriptDetails;
        return this;
    }

    public DtoFile getDtoFile() {
        return dtoFile;
    }

    public FileNode setDtoFile(DtoFile dtoFile) {
        this.dtoFile = dtoFile;
        return this;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public FileNode setChildren(List<FileNode> children) {
        this.children = children;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isFolder() {
        return folder;
    }

    public boolean ifPdf() {
        return dtoFile.isPdf();
    }


    @Override
    public String toString() {
        return "FileNode{" +
                "dtoFile=" + dtoFile +
                '}';
    }
}
