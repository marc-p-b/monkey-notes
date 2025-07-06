package net.kprod.mn.data.dto;

public class DtoGoogleDriveConnect {
    private String url;
    private boolean connected = true;

    public static DtoGoogleDriveConnect disconnected(String url) {
        return new DtoGoogleDriveConnect()
                .setConnected(false)
                .setUrl(url);
    }

    public String getUrl() {
        return url;
    }

    public boolean isConnected() {
        return connected;
    }


    private DtoGoogleDriveConnect setConnected(boolean connected) {
        this.connected = connected;
        return this;
    }

    private DtoGoogleDriveConnect setUrl(String url) {
        this.url = url;
        return this;
    }
}
