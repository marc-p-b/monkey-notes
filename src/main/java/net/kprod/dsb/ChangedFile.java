package net.kprod.dsb;

import com.google.api.services.drive.model.Change;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ChangedFile {
        private Change change;
        private UUID uuid;
        private OffsetDateTime timestamp;

        public ChangedFile(Change change) {
                this.change = change;
                uuid = UUID.randomUUID();
                timestamp = OffsetDateTime.now();
        }

        public Change getChange() {
                return change;
        }

        public UUID getUuid() {
                return uuid;
        }

        public OffsetDateTime getTimestamp() {
                return timestamp;
        }
}