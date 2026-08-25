<template>
  <div class="main-wrapper">

    <div class="page-header">
      <h2>Quicknotes</h2>
      <span v-if="notes.length" class="page-subtitle">
        {{ notes.length }} note{{ notes.length > 1 ? 's' : '' }}
        <template v-if="filtersActive">&middot; {{ filteredCount }} shown</template>
      </span>
      <Button
        icon="pi pi-refresh"
        text
        size="small"
        severity="secondary"
        class="header-refresh"
        v-tooltip.top="'Reload'"
        @click="fetchNotes"
      />
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner style="width: 2rem; height: 2rem" strokeWidth="6" />
    </div>

    <div v-else-if="error" class="empty-state">
      <i class="pi pi-exclamation-triangle"></i>
      {{ error }}
    </div>

    <div v-else-if="notes.length === 0" class="empty-state">
      <i class="pi pi-info-circle"></i>
      No quicknotes yet
    </div>

    <template v-else>
      <div class="filter-bar">
        <IconField class="filter-search">
          <InputIcon class="pi pi-search" />
          <InputText v-model="textFilter" placeholder="Filter notes..." />
        </IconField>

        <div v-if="tagValues.length || personValues.length" class="filter-chips">
          <Tag
            v-for="value in tagValues"
            :key="'tag-' + value"
            :severity="isSelected('tag', value) ? 'info' : 'secondary'"
            class="filter-chip"
            @click="toggleFilter('tag', value)"
          >
            <i class="pi pi-tag"></i> {{ value }}
          </Tag>
          <Tag
            v-for="value in personValues"
            :key="'person-' + value"
            :severity="isSelected('person', value) ? 'info' : 'secondary'"
            class="filter-chip"
            @click="toggleFilter('person', value)"
          >
            <i class="pi pi-user"></i> {{ value }}
          </Tag>
          <Button
            v-if="selectedFilters.length"
            label="Clear"
            icon="pi pi-times"
            text
            size="small"
            severity="secondary"
            @click="selectedFilters = []"
          />
        </div>
      </div>

      <div v-if="dayGroups.length === 0" class="empty-state">
        <i class="pi pi-filter-slash"></i>
        No note matches these filters
      </div>

      <div v-else class="feed">
        <section v-for="group in dayGroups" :key="group.key" class="day-group">
          <h3 class="day-label">{{ group.label }}</h3>

          <div v-for="note in group.notes" :key="note.uuid" class="page-card note-card">
            <span :id="'note' + note.uuid" class="note-anchor" />
            <div class="note-body" v-html="renderedBody(note)"></div>
            <div class="note-footer">
              <span class="note-time">{{ formatTime(note.createdAt) }}</span>
              <span class="note-source">
                <i :class="['pi', note.source === 'app' ? 'pi-mobile' : 'pi-desktop']"></i>
                {{ note.source }}
              </span>
            </div>
          </div>
        </section>
      </div>
    </template>

  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, nextTick } from "vue";
import { useRoute } from 'vue-router'
import { authFetch } from "@/requests";
import {
  renderNamedEntities,
  distinctValues,
  type RenderableNamedEntity
} from "@/utils/namedEntityRender";

interface NamedEntity extends RenderableNamedEntity {
  fileId: string
  fileName: string
  pageNumber: number
}

interface QuickNote {
  uuid: string
  body: string
  title: string | null
  createdAt: string
  updatedAt: string
  source: 'app' | 'web'
  listNamedEntities: NamedEntity[]
}

interface Filter {
  verb: string
  value: string
}

const route = useRoute()

const notes = ref<QuickNote[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const textFilter = ref('')
const selectedFilters = ref<Filter[]>([])

const allEntities = computed<NamedEntity[]>(() =>
  notes.value.flatMap(n => n.listNamedEntities ?? [])
)

const tagValues = computed(() => distinctValues(allEntities.value, 'tag'))
const personValues = computed(() => distinctValues(allEntities.value, 'person'))

const filtersActive = computed(() => selectedFilters.value.length > 0 || textFilter.value.trim() !== '')

function isSelected(verb: string, value: string): boolean {
  return selectedFilters.value.some(f => f.verb === verb && f.value === value)
}

function toggleFilter(verb: string, value: string) {
  if (isSelected(verb, value)) {
    selectedFilters.value = selectedFilters.value.filter(f => !(f.verb === verb && f.value === value))
  } else {
    selectedFilters.value = [...selectedFilters.value, { verb, value }]
  }
}

// Selecting several chips widens the result set rather than narrowing it: clicking two tags to get
// nothing back is the more surprising outcome.
function matchesFilters(note: QuickNote): boolean {
  const entities = note.listNamedEntities ?? []

  if (selectedFilters.value.length > 0) {
    const hit = selectedFilters.value.some(f =>
      entities.some(ne => ne.verb === f.verb && ne.value === f.value)
    )
    if (!hit) return false
  }

  const needle = textFilter.value.trim().toLowerCase()
  if (needle !== '' && !(note.body ?? '').toLowerCase().includes(needle)) {
    return false
  }

  return true
}

const filteredNotes = computed(() => notes.value.filter(matchesFilters))
const filteredCount = computed(() => filteredNotes.value.length)

interface DayGroup {
  key: string
  label: string
  notes: QuickNote[]
}

// The backend already returns newest-first, so grouping in insertion order keeps that ordering
// without re-sorting.
const dayGroups = computed<DayGroup[]>(() => {
  const groups = new Map<string, QuickNote[]>()
  filteredNotes.value.forEach(note => {
    const key = dayKey(note.createdAt)
    const bucket = groups.get(key)
    if (bucket) {
      bucket.push(note)
    } else {
      groups.set(key, [note])
    }
  })
  return [...groups.entries()].map(([key, list]) => ({
    key,
    label: dayLabel(key),
    notes: list
  }))
})

function dayKey(dateStr: string): string {
  if (!dateStr) return 'unknown'
  const d = new Date(dateStr)
  // local-date key, so a note taken late in the evening groups under the day the user experienced
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function dayLabel(key: string): string {
  if (key === 'unknown') return 'Undated'
  if (key === dayKey(new Date().toISOString())) return 'Today'

  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  if (key === dayKey(yesterday.toISOString())) return 'Yesterday'

  const [y, m, d] = key.split('-').map(Number)
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'full' }).format(new Date(y, m - 1, d))
}

//TODO common
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  return new Intl.DateTimeFormat('fr-FR', { timeStyle: 'short' }).format(new Date(dateStr))
}

function renderedBody(note: QuickNote): string {
  return renderNamedEntities(note.body, note.listNamedEntities)
}

async function fetchNotes() {
  loading.value = true
  error.value = null
  try {
    const response = await authFetch("quicknote/list")
    if (!response.ok) throw new Error("Network response was not ok")
    notes.value = await response.json()
  } catch (err: any) {
    console.error(err)
    error.value = "Failed to load quicknotes."
  } finally {
    loading.value = false
  }
}

// Arriving from a search hit: the feed loads async, so the browser can't act on the hash itself.
// Scroll once the notes are actually in the DOM.
async function scrollToHashAnchor() {
  if (!route.hash) return
  await nextTick()
  document.querySelector(route.hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(async () => {
  await fetchNotes()
  await scrollToHashAnchor()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.page-header h2 {
  margin: 0;
}

.page-subtitle {
  font-size: 0.875rem;
  color: var(--p-surface-500);
}

.header-refresh {
  margin-left: auto;
  align-self: center;
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

.filter-bar {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.filter-search {
  max-width: 22rem;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;
}

.filter-chip {
  cursor: pointer;
  user-select: none;
}

.feed {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.day-group {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.day-label {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-surface-500);
}

.page-card {
  background-color: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  border-radius: 0.5rem;
  overflow: hidden;
}

.note-card {
  position: relative;
}

/* offset the scroll target so a note linked from search isn't flush against the menubar */
.note-anchor {
  position: absolute;
  top: -4rem;
}

.note-body {
  padding: 0.9rem 1rem;
  font-size: 0.9rem;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.note-body :deep(h2),
.note-body :deep(h3),
.note-body :deep(h4),
.note-body :deep(h5),
.note-body :deep(h6) {
  margin: 0 0 0.4rem;
}

.note-footer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.4rem 1rem;
  background-color: var(--p-surface-50);
  border-top: 1px solid var(--p-surface-100);
  font-size: 0.75rem;
  color: var(--p-surface-500);
}

.note-source {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  margin-left: auto;
}
</style>
