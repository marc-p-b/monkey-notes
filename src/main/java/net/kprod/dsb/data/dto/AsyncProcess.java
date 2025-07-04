package net.kprod.dsb.data.dto;

import net.kprod.dsb.monitoring.AsyncResult;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

public class AsyncProcess {
        private String id;
        //private String uniqueId;
        private String name;
        private OffsetDateTime createdAt;
        private String description;
        private CompletableFuture<AsyncResult> future;

        public AsyncProcess() {
        }

        public String getId() {
                return id;
        }

        public AsyncProcess setId(String id) {
                this.id = id;
                return this;
        }
//
//        public String getUniqueId() {
//                return uniqueId;
//        }
//
//        public AsyncProcess setUniqueId(String uniqueId) {
//                this.uniqueId = uniqueId;
//                return this;
//        }

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
}