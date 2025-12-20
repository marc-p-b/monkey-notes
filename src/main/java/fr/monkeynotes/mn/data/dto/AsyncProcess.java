package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.enums.AsyncProcessName;
import fr.monkeynotes.mn.monitoring.AsyncResult;
import fr.monkeynotes.mn.monitoring.MonitoringData;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncProcess {
        private String id;
        private AsyncProcessName name;
        private String username;
        private OffsetDateTime createdAt;
        private String description;
        private CompletableFuture<AsyncResult> future;
        private List<AsyncProcessEvent> events;
        private List<AsyncProcessFileEvent> fileEvents;
        private boolean notified;

        public AsyncProcess(MonitoringData monitoringData, AsyncProcessName name, String username, String description) {
                this.id = monitoringData.getId();
                this.name = name;
                this.username = username;
                this.description = description;
                this.createdAt = OffsetDateTime.now();
                events = new ArrayList<>();
                fileEvents = new ArrayList<>();
        }

        public AsyncProcess setFuture(CompletableFuture<AsyncResult> future) {
                this.future = future;
                return this;
        }

        public String getUsername() {
                return username;
        }

        public AsyncProcess setUsername(String username) {
                this.username = username;
                return this;
        }

        public String getId() {
                return id;
        }


        public AsyncProcessName getName() {
                return name;
        }

        public OffsetDateTime getCreatedAt() {
                return createdAt;
        }


        public String getDescription() {
                return description;
        }


        public CompletableFuture<AsyncResult> getFuture() {
                return future;
        }


        public void addEvent(String event) {
                events.add(new AsyncProcessEvent(event));
        }

        public List<AsyncProcessEvent> getEvents() {
                return events;
        }

        public void addFileEvent(AsyncProcessFileEvent event) {
                fileEvents.add(event);
        }

        public List<AsyncProcessFileEvent> getFileEvents() {
                return fileEvents;
        }

        public void setNotified() {
                notified = true;
        }

        public boolean unNotified() {
                return !notified;
        }

        @Override
        public String toString() {
                return "AsyncProcess{" +
                        "id='" + id + '\'' +
                        ", name='" + name + '\'' +
                        ", createdAt=" + createdAt +
                        ", description='" + description + '\'' +
                        ", future=" + future +
                        '}';
        }
}