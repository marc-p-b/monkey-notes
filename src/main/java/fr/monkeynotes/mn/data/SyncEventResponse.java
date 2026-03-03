package fr.monkeynotes.mn.data;



public class SyncEventResponse {
    private String id;
    private SyncEventStatus status;
    private String message;

    public enum SyncEventStatus {
        accepted,
        refused
    }

    public static SyncEventResponse acceptedSyncEventResponse(String id) {
        return new SyncEventResponse()
                .setStatus(SyncEventStatus.accepted)
                .setMessage("Sync event accepted")
                .setId(id);
    }

    public static SyncEventResponse refusedSyncEventResponse(String message) {
        return new SyncEventResponse()
                .setStatus(SyncEventStatus.accepted)
                .setMessage(message)
                .setId("");
    }

    public String getId() {
        return id;
    }

    public SyncEventResponse setId(String id) {
        this.id = id;
        return this;
    }

    public SyncEventStatus getStatus() {
        return status;
    }

    public SyncEventResponse setStatus(SyncEventStatus status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public SyncEventResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}
