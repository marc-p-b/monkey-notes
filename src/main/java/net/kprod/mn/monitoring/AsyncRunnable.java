package net.kprod.mn.monitoring;

@FunctionalInterface
public interface AsyncRunnable<E extends Exception> extends Runnable {

    @Override
    default void run() throws RuntimeException {
        try {
            runThrows();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void runThrows() throws E;
}