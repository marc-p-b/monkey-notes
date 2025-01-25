package net.kprod.dsb.monitoring;

public class AsyncResult {
    private Long runTime;
    private Exception exception;

    private AsyncResult() {
    }

    public static AsyncResult success(long runTime) {
        return new AsyncResult()
                .setRunTime(runTime);
    }

    public static AsyncResult failure(long runTime, Exception e) {
        return new AsyncResult()
                .setRunTime(runTime)
                .setException(e);
    }

    public Long getRunTime() {
        return runTime;
    }

    private AsyncResult setRunTime(Long runTime) {
        this.runTime = runTime;
        return this;
    }

    public Exception getException() {
        return exception;
    }

    private AsyncResult setException(Exception exception) {
        this.exception = exception;
        return this;
    }

    public boolean isSuccessful() {
        return exception == null;
    }
}

