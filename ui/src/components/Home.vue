<template>
  <div class="home-wrapper">
    <div class="home-layout">

      <div class="recent-panel">
        <div class="panel-header">Recent</div>
        <ul class="recent-list">
          <li v-for="transcript in transcripts" :key="transcript.fileId">
            <a href="#" @click.prevent="clickedTranscript(transcript.transcript.fileId)" class="recent-link">
              <i class="pi pi-file-edit"></i>
              <span>{{ transcript.transcript.name }}</span>
            </a>
          </li>
        </ul>
      </div>

      <div class="tree-panel">
        <div class="panel-header">Documents</div>
        <TreeView @loading-status="loadingStatus" />
      </div>

    </div>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'Home' });
import TreeView from "@/components/TreeView.vue";
import { ref, onMounted } from "vue";
import { authFetch } from "@/requests";
import { useRouter } from 'vue-router'
const router = useRouter()
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

interface DtoTranscript {
  fileId: string;
  name: string;
}

const transcripts = ref<DtoTranscript[]>([])

let recentLoading: boolean = false
let foldersLoading: boolean = false

async function fetchRecentTranscripts() {
  recentLoading = true
  try {
    const response = await authFetch("transcript/recent");
    if (!response.ok) throw new Error("Network response was not ok");
    transcripts.value = await response.json();
  } catch (err: any) {
    console.error(err);
  } finally {
    recentLoading = false
    homeLoading()
  }
}

function clickedTranscript(fileId) {
  router.push({ name: 'transcript', params: { fileId } })
}

function loadingStatus(status: boolean) {
  foldersLoading = status
  homeLoading()
}

function homeLoading() {
  store.setLoading(foldersLoading || recentLoading)
}

onMounted(() => {
  fetchRecentTranscripts();
});
</script>

<style scoped>
.home-wrapper {
  height: calc(100vh - 60px);
  overflow: hidden;
}

.home-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  height: 100%;
}

.recent-panel {
  border-right: 1px solid var(--p-surface-200);
  overflow-y: auto;
  padding: 1rem;
}

.tree-panel {
  overflow-y: auto;
  padding: 1rem;
}

.panel-header {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--p-text-muted-color);
  margin-bottom: 0.6rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--p-surface-200);
}

.recent-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.recent-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.5rem;
  border-radius: 6px;
  text-decoration: none;
  color: var(--p-text-color);
  font-size: 0.85rem;
  transition: background 0.12s;
}

.recent-link:hover {
  background-color: var(--p-content-hover-background);
}

.recent-link i {
  color: var(--p-primary-color);
  font-size: 0.85rem;
  flex-shrink: 0;
}

.recent-link span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>