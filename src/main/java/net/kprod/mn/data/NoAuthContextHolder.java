package net.kprod.mn.data;

public class NoAuthContextHolder {
    private static final ThreadLocal<NoAuthContext> contextHolder = new ThreadLocal<>();

    public static void setContext(NoAuthContext context) {
        contextHolder.set(context);
    }

    public static NoAuthContext getContext() {
        return contextHolder.get();
    }

    public static void clearContext() {
        contextHolder.remove();
    }
}