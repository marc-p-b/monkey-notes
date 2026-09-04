<template>
  <div class="home-wrapper">

    <div class="page-header">
      <div>
        <h2>Documents</h2>
        <span class="page-subtitle" v-if="counts">{{ counts.transcripts }} documents &middot; {{ counts.folders }} folders</span>
      </div>
    </div>

    <div class="action-row">
      <SelectButton
          v-model="viewMode"
          :options="viewOptions"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
          size="small"
          dataKey="value"
      >
        <template #option="slotProps">
          <i :class="slotProps.option.icon" v-tooltip.top="slotProps.option.label"></i>
        </template>
      </SelectButton>
      <Button
          :label="selectMode ? 'Exit Select' : 'Select'"
          :icon="selectMode ? 'pi pi-times' : 'pi pi-check-square'"
          :severity="selectMode ? 'primary' : 'secondary'"
          size="small"
          outlined
          @click="selectMode = !selectMode"
      />
      <Select v-if="viewMode === 'folders'" v-model="orderBy" :options="orderOptions" optionLabel="label" optionValue="value" size="small" />
      <Button
          :icon="activeOrderDir === 'asc' ? 'pi pi-sort-amount-up' : 'pi pi-sort-amount-down'"
          size="small"
          outlined
          severity="secondary"
          @click="activeOrderDir = activeOrderDir === 'asc' ? 'desc' : 'asc'"
          v-tooltip.top="activeOrderDir === 'asc' ? 'Ascending' : 'Descending'"
      />
      <Button
          v-if="selectMode && selectedIds.size > 0"
          label="Actions"
          icon="pi pi-chevron-down"
          iconPos="right"
          size="small"
          outlined
          severity="secondary"
          @click="(e) => actionsMenu?.toggle(e)"
      />
      <Menu ref="actionsMenu" :model="bulkActionItems" popup />
    </div>

    <div class="home-layout">

      <div class="recent-panel">
        <div class="panel-header">Recent</div>
        <ul class="recent-list">
          <li v-for="transcript in transcripts" :key="transcript.fileId">
            <a href="#" @click.prevent="clickedTranscript(transcript.transcript.fileId)" class="recent-link">
              <i class="pi pi-file-edit"></i>
              <span>{{ transcript.transcript.title }}</span>
            </a>
          </li>
        </ul>
      </div>

      <div class="tree-panel">
        <TreeView
            v-if="viewMode === 'folders'"
            @loading-status="loadingStatus"
            :select-mode="selectMode"
            :order-by="orderBy"
            :order-dir="orderDir"
        />
        <DateView
            v-else
            @loading-status="loadingStatus"
            @transcript-clicked="clickedTranscript"
            :select-mode="selectMode"
            :order-dir="dateOrderDir"
        />
      </div>

    </div>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'Home' });
import TreeView from "@/components/TreeView.vue";
import DateView from "@/components/DateView.vue";
import { ref, reactive, computed, provide, watch, onMounted } from "vue";
import { authFetch } from "@/requests";
import { useRouter } from 'vue-router'
const router = useRouter()
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

interface DtoTranscript {
  fileId: string;
  name: string;
}

interface DtoCounts {
  folders: number;
  transcripts: number;
}

const transcripts = ref<DtoTranscript[]>([])
const counts = ref<DtoCounts | null>(null)

const selectMode = ref(false)
const viewMode = ref<'folders' | 'date'>('folders')
const viewOptions = [
  { label: 'Folders', value: 'folders', icon: 'pi pi-folder' },
  { label: 'By date', value: 'date', icon: 'pi pi-calendar' },
]
const orderBy = ref<'name' | 'date'>('name')
const orderDir = ref<'asc' | 'desc'>('asc')
//the date view keeps its own direction: A-Z is the natural default for a folder listing, newest-first
//for a chronological one. One button drives both, each view remembers what it was last set to.
const dateOrderDir = ref<'asc' | 'desc'>('desc')
const activeOrderDir = computed<'asc' | 'desc'>({
  get: () => viewMode.value === 'folders' ? orderDir.value : dateOrderDir.value,
  set: (dir) => {
    if (viewMode.value === 'folders') orderDir.value = dir
    else dateOrderDir.value = dir
  },
})
const orderOptions = [
  { label: 'Name', value: 'name' },
  { label: 'Date', value: 'date' },
]

const actionsMenu = ref()
const selectedIds = reactive(new Set<string>())

function toggleSelectedId(id: string) {
  if (selectedIds.has(id)) selectedIds.delete(id)
  else selectedIds.add(id)
}

provide('selectedIds', selectedIds)
provide('toggleSelectedId', toggleSelectedId)

watch(selectMode, (on) => {
  if (!on) selectedIds.clear()
})

//the two panels don't list the same rows (folders are selectable, years aren't), so a selection made
//in one is meaningless in the other
watch(viewMode, () => selectedIds.clear())

function askAgent() {
  router.push({ name: 'agent', query: { ids: Array.from(selectedIds).join(',') } })
}

const bulkActionItems = computed(() => [
  { label: 'Ask Agent', icon: 'pi pi-bolt', command: askAgent },
])

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

async function fetchCounts() {
  try {
    const response = await authFetch("transcript/count");
    if (!response.ok) throw new Error("Network response was not ok");
    counts.value = await response.json();
  } catch (err: any) {
    console.error(err);
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
  fetchCounts();
});
</script>

<style scoped>
.home-wrapper {
  height: calc(100vh - 60px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 1rem 1rem 0 1rem;
}

.page-header h2 {
  margin: 0;
}

.page-subtitle {
  font-size: 0.875rem;
  color: var(--p-surface-500);
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.75rem 1rem;
}

.home-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  flex: 1;
  min-height: 0;
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