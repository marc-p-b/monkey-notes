package net.kprod.dsb;

import com.google.api.services.drive.model.Change;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

public class ChangedFile {
        private Change change;
        ScheduledFuture<?> future;
        private long timestamp;

        public ChangedFile(Change change) {
                this.change = change;
                timestamp = System.currentTimeMillis();
        }

        public Change getChange() {
                return change;
        }

        public long getTimestamp() {
                return timestamp;
        }

        public ScheduledFuture<?> getFuture() {
                return future;
        }

        public ChangedFile setFuture(ScheduledFuture<?> future) {
                this.future = future;
                return this;
        }
}