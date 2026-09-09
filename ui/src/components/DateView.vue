<template>
  <div v-if="error" class="empty-state">
    <i class="pi pi-exclamation-triangle"></i>
    {{ error }}
  </div>

  <div v-else-if="!loading && groups.length === 0" class="empty-state">
    <i class="pi pi-info-circle"></i>
    No documents yet — notes synced from your tablet appear here once processed
  </div>

  <ul v-else class="date-root">
    <li v-for="group in groups" :key="group.year" class="year-item">
      <div class="year-row" @click="toggleYear(group.year)">
        <i :class="['pi', isExpanded(group.year) ? 'pi-chevron-down' : 'pi-chevron-right', 'chevron']"></i>
        <i class="pi pi-calendar year-icon"></i>
        <span class="year-label">{{ group.year }}</span>
        <span class="year-count">{{ group.documents.length }}</span>
      </div>

      <ul v-if="isExpanded(group.year)" class="documents-list">
        <li v-for="document in group.documents" :key="document.fileId">
          <div class="document-row" @click="emit('transcript-clicked', document.fileId)">
            <Checkbox v-if="selectMode" :modelValue="isChecked(document.fileId)" binary
                      @update:modelValue="toggleSelectedId(document.fileId)" @click.stop />
            <span v-else class="chevron-space"></span>
            <i class="pi pi-file-edit file-icon"></i>
            <span class="document-main">
              <span class="document-name">{{ document.title }}</span>
              <span class="document-folder" v-if="showFolder(document)">
                <i class="pi pi-folder"></i>{{ document.folder }}
              </span>
            </span>
            <span class="document-date" v-if="document.date"><span class="document-age">{{ formatAge(document.date) }}</span>, {{ formatDayMonth(document.date) }}</span>
          </div>
        </li>
      </ul>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { authFetch } from "@/requests";
import { ref, reactive, computed, onMounted, inject } from "vue";
import { effectiveDate, formatAge, formatDayMonth } from "@/utils/documentDate";

interface DtoTranscriptDetails {
  transcript: {
    fileId: string;
    title: string;
    documented_at?: string | null;
    discovered_at?: string | null;
    transcripted_at?: string | null;
  };
  //null for a document whose parent folder row is gone — ViewService keeps the row rather than
  //dropping it, so the listing has to cope with a missing parent
  parent?: { name?: string | null } | null;
}

interface DatedDocument {
  fileId: string;
  title: string;
  folder: string | null;
  //null for a transcript that carries no usable timestamp at all — grouped under UNDATED_YEAR
  date: Date | null;
}

interface YearGroup {
  year: string;
  documents: DatedDocument[];
}

//a label, not a year: sorts and renders alongside the real ones without needing a second code path
const UNDATED_YEAR = 'Undated'

//mirrors ROOT_FOLDER in MonkeySyncService/UpdateService — the name the sync root is stored under
const ROOT_FOLDER_NAME = '/'

const props = withDefaults(defineProps<{
  selectMode?: boolean;
  orderDir?: 'asc' | 'desc';
}>(), {
  selectMode: false,
  orderDir: 'asc',
});

const emit = defineEmits<{
  (e: "loading-status", status: boolean): void;
  (e: "transcript-clicked", fileId: string): void;
}>();

const selectedIds = inject<Set<string>>('selectedIds', new Set())
const toggleSelectedId = inject<(id: string) => void>('toggleSelectedId', () => {})

const documents = ref<DatedDocument[]>([])
const error = ref<string | null>(null)
//same reasoning as TreeView: a fetch always fires on mount, so starting false flashes the empty state
const loading = ref(true)

//years the user has opened; seeded with the current one once the first response lands, so a fresh
//load shows this year's notes and leaves the archive folded
const expandedYears = reactive(new Set<string>())
const currentYear = String(new Date().getFullYear())

const groups = computed<YearGroup[]>(() => {
  const byYear = new Map<string, DatedDocument[]>()
  for (const document of documents.value) {
    const year = document.date ? String(document.date.getFullYear()) : UNDATED_YEAR
    if (!byYear.has(year)) byYear.set(year, [])
    byYear.get(year)!.push(document)
  }

  const sign = props.orderDir === 'asc' ? 1 : -1
  for (const list of byYear.values()) {
    list.sort((a, b) => sign * ((a.date?.getTime() ?? 0) - (b.date?.getTime() ?? 0)))
  }

  //undated always last, whichever direction the real years run in
  return [...byYear.entries()]
      .map(([year, docs]) => ({ year, documents: docs }))
      .sort((a, b) => {
        if (a.year === UNDATED_YEAR) return 1
        if (b.year === UNDATED_YEAR) return -1
        return sign * (Number(a.year) - Number(b.year))
      })
})

function isExpanded(year: string) {
  return expandedYears.has(year)
}

function toggleYear(year: string) {
  if (expandedYears.has(year)) expandedYears.delete(year)
  else expandedYears.add(year)
}

function isChecked(fileId: string) {
  return selectedIds.has(fileId)
}

/**
 * A document sitting directly in the sync root has no folder worth naming — every row would repeat
 * the same label. The root folder row is stored with "/" as its name on both sync paths
 * (MonkeySyncService.ROOT_FOLDER when the app creates it, UpdateService.ROOT_FOLDER for the Drive
 * inbound folder), while every other folder carries its own path or Drive name, so the name alone
 * identifies it — no extra request needed.
 */
function showFolder(document: DatedDocument) {
  return document.folder !== null && document.folder !== ROOT_FOLDER_NAME
}

async function fetchTranscripts() {
  loading.value = true
  error.value = null
  emit("loading-status", true);
  try {
    const response = await authFetch("transcript/list/all");
    if (!response.ok) throw new Error("Network response was not ok");
    const list: DtoTranscriptDetails[] = await response.json();
    documents.value = list.map(d => ({
      fileId: d.transcript.fileId,
      title: d.transcript.title,
      folder: d.parent?.name ?? null,
      date: effectiveDate(d.transcript),
    }))
    expandedYears.add(currentYear)
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load documents.";
  } finally {
    loading.value = false
    emit("loading-status", false);
  }
}

onMounted(() => {
  fetchTranscripts();
});

//expandedYears is deliberately kept across a refresh — the user's open/closed years are their
//own state, not server data
defineExpose({ refresh: fetchTranscripts });
</script>

<style scoped>
.date-root {
  list-style: none;
  padding: 0;
  margin: 0;
}

.year-item {
  list-style: none;
}

.documents-list {
  margin-left: 1.25rem;
  padding: 0;
  list-style: none;
}

/* rows mirror TreeNode.vue's .tree-row so both home views read as the same list */
.year-row,
.document-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: background 0.12s;
}

.year-row:hover,
.document-row:hover {
  background-color: var(--p-content-hover-background);
}

.chevron {
  font-size: 0.65rem;
  color: var(--p-text-muted-color);
  width: 10px;
  flex-shrink: 0;
}

.chevron-space {
  width: 10px;
  flex-shrink: 0;
}

.year-icon {
  font-size: 0.9rem;
  color: var(--p-primary-color);
  flex-shrink: 0;
}

.year-label {
  font-size: 0.875rem;
  font-weight: 600;
}

.year-count {
  font-size: 0.7rem;
  color: var(--p-text-muted-color);
  background-color: var(--p-surface-100);
  border-radius: 10px;
  padding: 0 0.4rem;
}

.file-icon {
  font-size: 0.9rem;
  color: var(--p-text-muted-color);
  flex-shrink: 0;
}

/* title and folder travel together on the left; the flex:1 sits on the wrapper so the date stays
   pinned right and the folder doesn't drift away from the title it belongs to */
.document-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
}

.document-name {
  flex: 0 1 auto;
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* the title is what identifies the row, so the folder is the one that gives way when space runs out */
.document-folder {
  flex: 0 1 auto;
  min-width: 0;
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-folder i {
  font-size: 0.7rem;
  margin-right: 0.25rem;
}

.document-date {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  flex-shrink: 0;
  white-space: nowrap;
}

.document-age {
  color: var(--p-text-color);
}

/* same declaration as TreeView.vue / QuickNotesView.vue, which each keep their own copy */
.empty-state {
  color: var(--p-surface-400);
  font-size: 0.875rem;
  font-style: italic;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
