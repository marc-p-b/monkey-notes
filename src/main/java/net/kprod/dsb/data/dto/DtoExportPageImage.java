package net.kprod.dsb.data.dto;

public class DtoExportPageImage {
    private String fileId;
    private int pageNumber;

    public DtoExportPageImage(String fileId, int pageNumber) {
        this.fileId = fileId;
        this.pageNumber = pageNumber;
    }

    public String getFileId() {
        return fileId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

}
