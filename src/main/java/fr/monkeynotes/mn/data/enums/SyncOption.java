package fr.monkeynotes.mn.data.enums;

public enum SyncOption {
    none("No Sync"),
    gdrive("Google Drive Sync"),
    monkey("Monkey Notes Companion App Sync");

    private final String label;

    SyncOption(String label) {
        this.label = label;
    }
}
