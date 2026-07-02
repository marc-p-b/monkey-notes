<template>
  <div class="main-wrapper">

    <div class="page-header">
      <h2>Processes</h2>
      <Button icon="pi pi-refresh" text severity="secondary" :loading="loading" @click="fetchProcesses" v-tooltip.bottom="'Refresh'" />
    </div>

    <div v-if="loading && processes.length === 0" class="loading-state">
      <ProgressSpinner style="width: 2rem; height: 2rem" strokeWidth="6" />
    </div>

    <div v-else-if="processes.length === 0" class="empty-state">
      <i class="pi pi-check-circle"></i>
      No process currently running
    </div>

    <div v-else class="process-list">
      <div v-for="process in processes" :key="process.id" class="page-card">
        <div class="page-card-header">
          <i :class="['pi', processIcon(process.name), 'process-icon']"></i>
          <span class="process-name">{{ processLabel(process.name) }}</span>
          <Tag :value="process.statusStr" :severity="statusSeverity(process.status)" class="process-status" />
          <Button
            v-if="process.status === 'running'"
            icon="pi pi-times"
            text
            size="small"
            severity="danger"
            @click="confirmCancel(process)"
            v-tooltip.top="'Cancel process'"
            class="process-cancel-btn"
          />
        </div>
        <div class="page-content">
          <p v-if="process.description" class="process-description">{{ process.description }}</p>
          <div class="properties-grid">
            <div class="property-row">
              <i class="pi pi-user property-icon"></i>
              <span class="property-label">User</span>
              <span class="property-value">{{ process.username }}</span>
            </div>
            <div class="property-row">
              <i class="pi pi-clock property-icon"></i>
              <span class="property-label">Duration</span>
              <span class="property-value">{{ process.duration }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ConfirmDialog />

  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from "vue";
import { authFetch } from "@/requests";
import { useConfirm } from "primevue/useconfirm";

const confirm = useConfirm();

interface DtoProcess {
  id: string
  name: string
  statusStr: string
  status: string
  description: string
  duration: string
  username: string
}

const PROCESS_LABELS: Record<string, string> = {
  flushChanges: "Flush changes",
  flushMonkeySyncs: "Flush MonkeySync",
  updateFolder: "Update folder",
  forcePageUpdate: "Force page update",
  forceTranscriptUpdate: "Force transcript update",
}

const PROCESS_ICONS: Record<string, string> = {
  flushChanges: "pi-sync",
  flushMonkeySyncs: "pi-sync",
  updateFolder: "pi-folder",
  forcePageUpdate: "pi-file-edit",
  forceTranscriptUpdate: "pi-file-edit",
}

function processLabel(name: string): string {
  return PROCESS_LABELS[name] || name
}

function processIcon(name: string): string {
  return PROCESS_ICONS[name] || "pi-cog"
}

function statusSeverity(status: string): string {
  switch (status) {
    case "running": return "info"
    case "completed": return "success"
    case "failed":
    case "error": return "danger"
    default: return "secondary"
  }
}

const processes = ref<DtoProcess[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
let pollHandle: ReturnType<typeof setInterval> | null = null

async function fetchProcesses() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("process/list");
    if (!response.ok) throw new Error("Network response was not ok");
    processes.value = await response.json();
    syncPolling();
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load processes.";
  } finally {
    loading.value = false;
  }
}

async function cancelProcess(id: string) {
  try {
    const response = await authFetch("process/cancel/" + id);
    if (!response.ok) throw new Error("Network response was not ok");
    await fetchProcesses();
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to cancel process";
  }
}

function confirmCancel(process: DtoProcess) {
  confirm.require({
    message: `Cancel "${processLabel(process.name)}"?`,
    header: "Confirm",
    icon: "pi pi-exclamation-triangle",
    acceptClass: "p-button-danger",
    accept: () => cancelProcess(process.id),
  });
}

function syncPolling() {
  const hasRunning = processes.value.some(p => p.status === "running")
  if (hasRunning && !pollHandle) {
    pollHandle = setInterval(fetchProcesses, 4000)
  } else if (!hasRunning && pollHandle) {
    clearInterval(pollHandle)
    pollHandle = null
  }
}

onMounted(() => {
  fetchProcesses();
});

onUnmounted(() => {
  if (pollHandle) clearInterval(pollHandle)
});

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

.process-list {
  display: flex;
  flex-direction: column;
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

.process-icon {
  color: var(--p-primary-500);
  font-size: 0.875rem;
}

.process-name {
  font-size: 0.875rem;
  font-weight: 500;
}

.process-status {
  margin-left: 0.25rem;
}

.process-cancel-btn {
  margin-left: auto;
}

.page-content {
  padding: 1rem;
}

.process-description {
  margin: 0 0 0.75rem 0;
  font-size: 0.875rem;
  color: var(--p-surface-600);
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