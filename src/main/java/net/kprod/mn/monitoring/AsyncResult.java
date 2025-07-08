package net.kprod.mn.monitoring;

public class AsyncResult {
    public enum State {
        //unknown,
        completed,
        failed;
    };
    private Long runTime;
    private Exception exception;
    private State state = State.failed;

    private AsyncResult() {
    }

    public static AsyncResult success(long runTime) {
        return new AsyncResult()
                .setState(State.completed)
                .setRunTime(runTime);
    }

    public static AsyncResult failure(long runTime, Exception e) {
        return new AsyncResult()
                .setState(State.failed)
                .setRunTime(runTime)
                .setException(e);
    }

    public State getState() {
        return state;
    }

    public AsyncResult setState(State state) {
        this.state = state;
        return this;
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
        return state == State.completed;
    }

    public boolean isFailure() {
        return state == State.failed;
    }

}

