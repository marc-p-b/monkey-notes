package fr.monkeynotes.mn.data.dto;

import java.time.OffsetDateTime;

public class AsyncProcessEvent {
    private OffsetDateTime timestamp;
    private String text;

    public AsyncProcessEvent(String text) {
        this.timestamp = OffsetDateTime.now();
        this.text = text;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getText() {
        return text;
    }
}
