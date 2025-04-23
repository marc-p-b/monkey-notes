package net.kprod.dsb.data;

import com.google.api.services.drive.model.Change;
import org.springframework.security.core.Authentication;

import java.util.concurrent.ScheduledFuture;

public class ChangedFile {
        private Change change;
        ScheduledFuture<?> future;
        private long timestamp;
        private Authentication auth;

        public ChangedFile(Change change, Authentication authentication) {
                this.change = change;
                this.auth = authentication;
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

        public Authentication getAuth() {
                return auth;
        }

        @Override
        public String toString() {
                return new StringBuilder().append("fileId ").append(change.getFileId()).append(" name ").append(change.getFile().getName()).toString();
        }
}