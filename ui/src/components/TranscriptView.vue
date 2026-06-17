<template>

  <div class="home-wrapper">
    <div v-if="loading" class="loading-state">
      <ProgressSpinner style="width: 2rem; height: 2rem" strokeWidth="6" />
    </div>

    <div v-else>
      <div class="transcript-header">
        <Button icon="pi pi-arrow-left" text severity="secondary" @click="router.back()" />
        <div class="transcript-header-text">
          <h1>{{ transcript.title }}</h1>
          <span class="transcript-subtitle">{{ transcript.pages.length }} pages &middot; transcribed {{ formatDate(transcript.transcripted_at) }}</span>
        </div>
      </div>

      <div class="transcript-info">
        <Tabs value="0">
          <TabList>
            <Tab value="0">Properties</Tab>
            <Tab value="1">Tags</Tab>
            <Tab value="2">TOC</Tab>
          </TabList>
          <TabPanels>
            <TabPanel value="0">
              <div class="properties-grid">
                <div class="property-row">
                  <i class="pi pi-clock property-icon"></i>
                  <span class="property-label">Transcribed</span>
                  <span class="property-value">{{ formatDate(transcript.transcripted_at) }}</span>
                </div>
                <div class="property-row">
                  <i class="pi pi-file-edit property-icon"></i>
                  <span class="property-label">Documented</span>
                  <span class="property-value">{{ formatDate(transcript.documented_at) }}</span>
                </div>
                <div class="property-row">
                  <i class="pi pi-search property-icon"></i>
                  <span class="property-label">Discovered</span>
                  <span class="property-value">{{ formatDate(transcript.discovered_at) }}</span>
                </div>
                <div class="property-row">
                  <i class="pi pi-copy property-icon"></i>
                  <span class="property-label">Pages</span>
                  <span class="property-value">{{ transcript.pages.length }}</span>
                </div>
                <div class="property-row">
                  <i class="pi pi-history property-icon"></i>
                  <span class="property-label">Version</span>
                  <span class="property-value">{{ transcript.version }}</span>
                </div>
              </div>
              <div class="action-row">
                <Button @click.prevent="toggleEditModeRequest()" :label="store.transcript_edit_mode ? 'Lock' : 'Edit'" :icon="stateEditIcon" :severity="stateEditSeverity" size="small" outlined />
                <Button @click.prevent="agent(transcript.fileId)" label="Agent" icon="pi pi-bolt" size="small" outlined severity="secondary" />
                <Button @click.prevent="updateTranscript(transcript.fileId)" label="Update" icon="pi pi-refresh" size="small" outlined severity="secondary" />
                <Button @click.prevent="downloadFile(transcript.fileId)" label="PDF" icon="pi pi-download" size="small" outlined severity="secondary" />
              </div>
            </TabPanel>

            <TabPanel value="1">
              <div v-if="!transcript.tagsMap || Object.keys(transcript.tagsMap).length === 0" class="empty-state">
                No tags
              </div>
              <div v-else class="tags-grid">
                <div v-for="(tags, name) in transcript.tagsMap" :key="name" class="tag-group">
                  <span class="tag-name">{{ name }}</span>
                  <div class="tag-pages">
                    <a v-for="tag in tags" :key="tag.uuid" :href="'#' + tag.uuid" class="tag-page-link">
                      <Tag :value="'p. ' + (tag.pageNumber + 1)" severity="secondary" />
                    </a>
                  </div>
                </div>
              </div>
            </TabPanel>

            <TabPanel value="2">
              <div v-if="transcript.toc.length === 0" class="empty-state">No table of contents</div>
              <ul v-else class="toc">
                <li v-for="item in transcript.toc" :key="item.uuid"
                    :style="{ paddingLeft: getIndent(item.verb) + 'px' }"
                    :class="'toc-item toc-' + item.verb">
                  {{ item.value }}
                  <a :href="'#' + item.uuid" class="toc-link"><i class="pi pi-link"></i></a>
                </li>
              </ul>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </div>

      <div v-for="page in transcript.pages" :key="page.pageNumber" class="page-card">
        <div class="page-card-header">
          <span class="page-badge">Page {{ page.pageNumber + 1 }}</span>
        </div>
        <div class="page-content">
          <TranscriptPage :page="page" :activeEditPageNumber="activeEditPageNumber" @requestEdit="handleEditRequest" />
        </div>
      </div>
    </div>
  </div>

</template>

<style scoped>

.loading-state {
  display: flex;
  justify-content: center;
  padding: 4rem 0;
}

.transcript-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.transcript-header-text {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.transcript-header-text h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 500;
  line-height: 1.2;
}

.transcript-subtitle {
  font-size: 0.875rem;
  color: var(--p-surface-500);
}

.transcript-info {
  background-color: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  border-radius: 0.5rem;
  margin-bottom: 1rem;
  overflow: hidden;
}

:deep(.p-dark) .transcript-info {
  background-color: var(--p-surface-900);
  border-color: var(--p-surface-700);
}

.properties-grid {
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
}

.property-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
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

.action-row {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* Tags */
.tags-grid {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.tag-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.tag-name {
  font-size: 0.875rem;
  font-weight: 500;
  min-width: 7rem;
  color: var(--p-surface-700);
}

.tag-pages {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.tag-page-link {
  text-decoration: none;
}

.empty-state {
  color: var(--p-surface-400);
  font-size: 0.875rem;
  font-style: italic;
}

/* TOC */
.toc {
  list-style: none;
  padding-left: 0;
  margin: 0;
}

.toc-item {
  line-height: 1.8;
  font-size: 0.9rem;
}

.toc-h2 {
  font-weight: 600;
  font-size: 1rem !important;
}

.toc-h3 {
  font-weight: 500;
}

.toc-link {
  margin-left: 0.4rem;
  color: var(--p-surface-400);
  font-size: 0.8em;
  text-decoration: none;
}

.toc-link:hover {
  color: var(--p-primary-500);
}

/* Page cards */
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
  padding: 0.5rem 1rem;
  background-color: var(--p-surface-50);
  border-bottom: 1px solid var(--p-surface-200);
}

.page-badge {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--p-surface-400);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.page-content {
  padding: 1rem;
}

</style>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { authFetch } from "@/requests.ts";
import TranscriptPage from "./TranscriptPage.vue";
import { useRouter } from 'vue-router'
const router = useRouter()

import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

const props = defineProps<{
  fileId: string
  pageNumber: number
}>()

const loading = ref(true)
const error = ref<string | null>(null)

const transcript = ref<DtoTranscript>(null)
const activeEditPageNumber = ref<number | null>(null)

const stateEditIcon = ref<string>()
const stateEditSeverity = ref<string>()


interface DtoTranscript {
  username: string
  fileId: string
  name: string
  transcripted_at: string
  documented_at: string
  discovered_at: string
  pageCount: number
  version: number
  pages: Page[]
  title: string
  tags: NamedEntity[]
  toc: NamedEntity[]
  tagsMap: Record<string, NamedEntity[]>;
}

interface NamedEntity {
  uuid: string
  verb: string
  value: string
  fileId: string
  fileName: string
  pageNumber: number
  start: number
  end: number
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date)
}

function getIndent(verb: string): number {
  switch (verb) {
    case 'h2': return 0
    case 'h3': return 16
    case 'h4': return 32
    case 'h5': return 48
    case 'h6': return 64
    default: return 0
  }
}

async function fetchTranscript() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("transcript/" + props.fileId);
    if (!response.ok) throw new Error("Network response was not ok");
    transcript.value = await response.json();
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }
}

async function updateTranscript(fileId) {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("transcript/update/" + fileId);
    if (!response.ok) throw new Error("Network response was not ok");
    console.log(response)
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to update transcript.";
  } finally {
    loading.value = false;
  }
}

const downloadFile = async (fileId: string) => {
  try {
    const response = await authFetch('transcript/pdf/' + props.fileId)
    if (!response.ok) throw new Error(`Server error: ${response.status}`)

    const blob = await response.blob()
    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = 'downloaded-file'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="(.+)"/)
      if (match) fileName = match[1]
    }

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    console.error('Download failed:', err)
  }
}

function agent(fileId) {
  router.push({ name: 'agent', params: { fileId } })
}

function syncEditButtonState() {
  stateEditIcon.value = store.transcript_edit_mode ? "pi pi-lock-open" : "pi pi-lock"
  stateEditSeverity.value = store.transcript_edit_mode ? "warn" : "secondary"
}

function toggleEditModeRequest() {
  if (store.transcript_edit_mode) {
    store.transcriptViewMode()
    activeEditPageNumber.value = null
  } else {
    store.transcriptEditMode()
  }
  syncEditButtonState()
}

function handleEditRequest(pageNumber: number, isClosing: boolean) {
  if (isClosing) {
    activeEditPageNumber.value = null
  } else {
    activeEditPageNumber.value = pageNumber
  }
}

onMounted(() => {
  fetchTranscript()
  store.transcriptViewMode()
  syncEditButtonState()
});

</script>