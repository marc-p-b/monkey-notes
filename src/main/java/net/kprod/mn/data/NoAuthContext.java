package net.kprod.mn.data;

public class NoAuthContext {
    private String username;

    public NoAuthContext(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
