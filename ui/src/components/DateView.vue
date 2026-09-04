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
            <span class="document-name">{{ document.title }}</span>
            <span class="document-date">{{ formatDayMonth(document.date) }}</span>
          </div>
        </li>
      </ul>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { authFetch } from "@/requests";
import { ref, reactive, computed, onMounted, inject } from "vue";

interface DtoTranscriptDetails {
  transcript: {
    fileId: string;
    title: string;
    documented_at?: string | null;
    discovered_at?: string | null;
    transcripted_at?: string | null;
  };
}

interface DatedDocument {
  fileId: string;
  title: string;
  //null for a transcript that carries no usable timestamp at all — grouped under UNDATED_YEAR
  date: Date | null;
}

interface YearGroup {
  year: string;
  documents: DatedDocument[];
}

//a label, not a year: sorts and renders alongside the real ones without needing a second code path
const UNDATED_YEAR = 'Undated'

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

/**
 * The date a note is filed under. documented_at is the date written on the note itself (parsed from
 * the title by the OCR pipeline) and is what the user means by "when is this note from";
 * discovered_at (first sync) and transcripted_at (last OCR run) are only fallbacks for a note whose
 * title carried no date.
 */
function effectiveDate(transcript: DtoTranscriptDetails['transcript']): Date | null {
  const raw = transcript.documented_at ?? transcript.discovered_at ?? transcript.transcripted_at
  if (!raw) return null
  const date = new Date(raw)
  return isNaN(date.getTime()) ? null : date
}

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

function formatDayMonth(date: Date | null) {
  if (!date) return ''
  return date.toLocaleDateString(undefined, { day: '2-digit', month: 'short' })
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

.document-name {
  flex: 1;
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-date {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  flex-shrink: 0;
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
