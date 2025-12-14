package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.monitoring.AsyncResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncProcess {
        private String id;
        private String name;
        private OffsetDateTime createdAt;
        private String description;
        private CompletableFuture<AsyncResult> future;
        private List<AsyncProcessEvent> events;
        private List<AsyncProcessFileEvent> fileEvents;

        public AsyncProcess() {
        }

        public String getId() {
                return id;
        }

        public AsyncProcess setId(String id) {
                this.id = id;
                return this;
        }

        public String getName() {
                return name;
        }

        public AsyncProcess setName(String name) {
                this.name = name;
                return this;
        }

        public OffsetDateTime getCreatedAt() {
                return createdAt;
        }

        public AsyncProcess setCreatedAt(OffsetDateTime createdAt) {
                this.createdAt = createdAt;
                return this;
        }

        public String getDescription() {
                return description;
        }

        public AsyncProcess setDescription(String description) {
                this.description = description;
                return this;
        }

        public CompletableFuture<AsyncResult> getFuture() {
                return future;
        }

        public AsyncProcess setFuture(CompletableFuture<AsyncResult> future) {
                this.future = future;
                return this;
        }

        public void addEvent(String event) {
                if (events == null) {
                        events = new ArrayList<>();
                }
                events.add(new AsyncProcessEvent(event));
        }

        public List<AsyncProcessEvent> getEvents() {
                return events;
        }

        public void addFileEvent(AsyncProcessFileEvent event) {
                if (fileEvents == null) {
                        fileEvents = new ArrayList<>();
                }
                fileEvents.add(event);
        }

        public List<AsyncProcessFileEvent> getFileEvents() {
                return fileEvents;
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