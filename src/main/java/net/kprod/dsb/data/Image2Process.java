package net.kprod.dsb.data;

import java.net.URL;

public class Image2Process {
    private String fileId;
    private int pageNumber;
    private URL url;

    public static Image2Process create(String fileId, int pageNumber, URL url) {
        return new Image2Process()
                .setFileId(fileId)
                .setPageNumber(pageNumber)
                .setUrl(url);
    }

    public String getFileId() {
        return fileId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public URL getUrl() {
        return url;
    }

    private Image2Process setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    private Image2Process setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    private Image2Process setUrl(URL url) {
        this.url = url;
        return this;
    }

}