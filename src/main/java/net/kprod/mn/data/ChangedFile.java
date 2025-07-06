package net.kprod.mn.data;

import com.google.api.services.drive.model.Change;

import java.util.concurrent.ScheduledFuture;

public class ChangedFile {
        private Change change;
        private ScheduledFuture<?> future;
        private long timestamp;
        //private Authentication auth;
        private String username;

        public ChangedFile(Change change, String username) {
                this.change = change;
                //this.auth = authentication;
                this.username = username;
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

        public String getUsername() {
                return username;
        }

        @Override
        public String toString() {
                return new StringBuilder().append("fileId ").append(change.getFileId()).append(" name ").append(change.getFile().getName()).toString();
        }
}