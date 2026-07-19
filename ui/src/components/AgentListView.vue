<template>
  <div class="main-wrapper">

    <div class="page-header">
      <h2>Agent Conversations</h2>
      <Button icon="pi pi-refresh" text severity="secondary" :loading="loading" @click="fetchThreads" v-tooltip.bottom="'Refresh'" />
    </div>

    <div v-if="loading && threads.length === 0" class="loading-state">
      <ProgressSpinner style="width: 2rem; height: 2rem" strokeWidth="6" />
    </div>

    <div v-else-if="threads.length === 0" class="empty-state">
      <i class="pi pi-comments"></i>
      No agent conversations yet
    </div>

    <div v-else class="thread-list">
      <div
        v-for="thread in threads"
        :key="thread.uuid"
        class="page-card thread-card"
        @click="openThread(thread)"
      >
        <div class="page-card-header">
          <i class="pi pi-comments thread-icon"></i>
          <span class="thread-name">{{ thread.threadName || 'Untitled conversation' }}</span>
        </div>
        <div class="page-content">
          <div class="properties-grid">
            <div class="property-row">
              <i class="pi pi-calendar-plus property-icon"></i>
              <span class="property-label">Created</span>
              <span class="property-value">{{ formatDate(thread.createdAt) }}</span>
            </div>
            <div class="property-row">
              <i class="pi pi-clock property-icon"></i>
              <span class="property-label">Last update</span>
              <span class="property-value">{{ formatDate(thread.lastThreadUpdate) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue"
import { useRouter } from 'vue-router'
import { authFetch } from "@/requests"

const router = useRouter()

interface DtoAgentPrepare {
  uuid: string
  threadName?: string
  createdAt?: string
  lastThreadUpdate?: string
}

const threads = ref<DtoAgentPrepare[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

function formatDate(iso?: string) {
  if (!iso) return ''
  return new Date(iso).toLocaleString()
}

async function fetchThreads() {
  loading.value = true
  error.value = null
  try {
    const response = await authFetch("agent/list")
    if (!response.ok) throw new Error("Network response was not ok")
    threads.value = await response.json()
  } catch (err: any) {
    console.error(err)
    error.value = "Failed to load agent conversations."
  } finally {
    loading.value = false
  }
}

function openThread(thread: DtoAgentPrepare) {
  router.push({ name: 'agent', query: { uuid: thread.uuid } })
}

onMounted(() => {
  fetchThreads()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.page-header h2 {
  margin: 0;
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 4rem 0;
}

.empty-state {
  color: var(--p-surface-400);
  font-size: 0.875rem;
  font-style: italic;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.thread-list {
  display: flex;
  flex-direction: column;
}

.thread-card {
  cursor: pointer;
  transition: border-color 0.12s;
}

.thread-card:hover {
  border-color: var(--p-primary-color);
}

.page-card {
  background-color: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  border-radius: 0.5rem;
  margin-bottom: 1rem;
  overflow: hidden;
}

.page-card-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 1rem;
  background-color: var(--p-surface-50);
  border-bottom: 1px solid var(--p-surface-200);
}

.thread-icon {
  color: var(--p-primary-500);
  font-size: 0.875rem;
}

.thread-name {
  font-size: 0.875rem;
  font-weight: 500;
}

.page-content {
  padding: 1rem;
}

.properties-grid {
  display: flex;
  flex-direction: column;
}

.property-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.4rem 0;
  border-bottom: 1px solid var(--p-surface-100);
}

.property-row:last-child {
  border-bottom: none;
}

.property-icon {
  color: var(--p-primary-500);
  width: 1rem;
  text-align: center;
  font-size: 0.875rem;
}

.property-label {
  flex: 0 0 7rem;
  font-size: 0.875rem;
  color: var(--p-surface-500);
}

.property-value {
  font-size: 0.875rem;
}
</style>