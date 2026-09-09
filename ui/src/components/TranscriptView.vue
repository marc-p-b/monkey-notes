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
          <span class="transcript-subtitle">{{ transcript.pages.length }} pages &middot; created {{ formatDate(transcript.documented_at) }}</span>
        </div>
        <div class="action-row">
          <Button @click.prevent="toggleAllImages()" :label="allImagesShown ? 'Hide Images' : 'Show Images'" icon="pi pi-image" :severity="allImagesShown ? 'primary' : 'secondary'" size="small" outlined />
          <Button @click.prevent="agent(transcript.fileId)" label="Agent" icon="pi pi-bolt" size="small" outlined severity="secondary" />
          <Button
              :label="updateLabel"
              :icon="updateIcon"
              :severity="updateState === 'failed' ? 'danger' : 'secondary'"
              size="small"
              outlined
              @click="(e) => updateMenu?.toggle(e)"
          />
          <Menu ref="updateMenu" :model="updateActionItems" popup />
          <Button @click.prevent="downloadFile(transcript.fileId)" label="PDF" icon="pi pi-download" size="small" outlined severity="secondary" />
          <Button
              :label="copyLabel"
              :icon="copyIcon"
              :severity="copyState === 'failed' ? 'danger' : 'secondary'"
              size="small"
              outlined
              @click="(e) => copyMenu?.toggle(e)"
          />
          <Menu ref="copyMenu" :model="copyActionItems" popup />
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
                  <i class="pi pi-file property-icon"></i>
                  <span class="property-label">Name</span>
                  <!-- the file as it arrived from the tablet; the h1 above shows the parsed title,
                       which is usually not the same string -->
                  <span class="property-value">{{ transcript.name }}</span>
                </div>
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

      <div v-for="(page, index) in transcript.pages" :key="page.pageNumber" class="page-card">
        <div class="page-card-header">
          <span v-if="page.pageDiagram == PageDiagram.full" class="page-badge">Page {{ page.pageNumber + 1 }} - Diagram : {{ page.diagramTitle }}</span>
          <span v-else-if="page.pageDiagram == PageDiagram.inline" class="page-badge">(Page {{ page.pageNumber + 1 }} Diagram : {{ page.diagramTitle }})</span>
          <span v-else="page.pageDiagram == PageDiagram.full" class="page-badge">Page {{ page.pageNumber + 1 }}</span>
          <!-- editing actions sit on the left, next to the page number, so they read as being about
               this page rather than joining the view/image controls pinned on the right -->
          <div v-if="activeEditPageNumber === page.pageNumber" class="page-edit-actions">
            <Button label="Save" size="small" @click="pageRefs[page.pageNumber]?.save()" />
            <Button label="Reset" outlined size="small" severity="secondary" @click="pageRefs[page.pageNumber]?.reset()" />
          </div>
          <div class="page-header-actions">
            <Button
              icon="pi pi-image"
              text
              size="small"
              :severity="pageShowImages[page.pageNumber] ? 'primary' : 'secondary'"
              @click="togglePageImage(page.pageNumber)"
              v-tooltip.top="pageShowImages[page.pageNumber] ? 'Hide image' : 'Show image'"
            />
            <Button
              icon="pi pi-pencil"
              text
              size="small"
              :severity="activeEditPageNumber === page.pageNumber ? 'warn' : 'secondary'"
              @click="handleEditRequest(page.pageNumber, activeEditPageNumber === page.pageNumber)"
              v-tooltip.top="activeEditPageNumber === page.pageNumber ? 'Close edit' : 'Edit page'"
            />
          </div>
        </div>
        <div v-if="page.pageDiagram != PageDiagram.inline" class="page-content">
          <TranscriptPage :ref="el => setPageRef(page.pageNumber, el)" :page="page" :nextPage="transcript.pages[index + 1] ?? null" :activeEditPageNumber="activeEditPageNumber" :showImages="!!pageShowImages[page.pageNumber]" @requestEdit="handleEditRequest" @pageReady="handlePageReady" />
        </div>
      </div>
    </div>
  </div>

</template>


<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from "vue";
import { authFetch } from "@/requests.ts";
import TranscriptPage from "./TranscriptPage.vue";
import { useRouter, useRoute } from 'vue-router'
const router = useRouter()
const route = useRoute()


const props = defineProps<{
  fileId: string
  pageNumber: number
}>()

const loading = ref(true)
const error = ref<string | null>(null)

const transcript = ref<DtoTranscript>(null)
const activeEditPageNumber = ref<number | null>(null)
const pageShowImages = ref<Record<number, boolean>>({})
const allImagesShown = ref(false)

const copyMenu = ref()
const copyState = ref<'idle' | 'copied' | 'failed'>('idle')
let copyStateTimer: ReturnType<typeof setTimeout> | undefined

//labels are static, so a plain array rather than a computed
const copyActionItems = [
  { label: 'Copy raw', icon: 'pi pi-align-left', command: copyRaw },
  { label: 'Copy as MD', icon: 'pi pi-hashtag', command: copyAsMarkdown },
]

const updateMenu = ref()
const updateState = ref<'idle' | 'requested' | 'failed'>('idle')
let updateStateTimer: ReturnType<typeof setTimeout> | undefined

//same as copyActionItems: static labels, and the commands are arrows over hoisted function
//declarations, so the array can be built here above them
const updateActionItems = [
  { label: 'Post process only', icon: 'pi pi-sparkles', command: () => postProcessTranscript(props.fileId) },
  { label: 'Update including OCR', icon: 'pi pi-refresh', command: () => updateTranscript(props.fileId) },
]

const updateLabel = computed(() => {
  if (updateState.value === 'requested') return 'Requested'
  if (updateState.value === 'failed') return 'Update failed'
  return 'Update'
})

const updateIcon = computed(() => {
  if (updateState.value === 'requested') return 'pi pi-check'
  if (updateState.value === 'failed') return 'pi pi-times'
  return 'pi pi-refresh'
})

const copyLabel = computed(() => {
  if (copyState.value === 'copied') return 'Copied'
  if (copyState.value === 'failed') return 'Copy failed'
  return 'Copy'
})

const copyIcon = computed(() => {
  if (copyState.value === 'copied') return 'pi pi-check'
  if (copyState.value === 'failed') return 'pi pi-times'
  return 'pi pi-copy'
})

enum PageDiagram {
  none = 'none',
  full = 'full',
  inline = 'inline'
}

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

//TODO common
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

//re-runs the OCR itself, then everything derived from it
async function updateTranscript(fileId: string) {
  await requestProcess("transcript/update/" + fileId, "Failed to update transcript.")
}

//re-derives from the stored transcript only — no OCR call, so no cost and no risk of a worse
//transcription; this is what picks up a change to the named entity rules
async function postProcessTranscript(fileId: string) {
  await requestProcess("transcript/postprocess/" + fileId, "Failed to post process transcript.")
}

/**
 * Both endpoints only queue an async process and return immediately, so `loading` is deliberately
 * not toggled here — blanking the view behind the spinner would say the work is done when the
 * response lands, and it isn't. The button reports instead, the way the Copy menu does, since this
 * view's `error` ref is never rendered anywhere. Progress belongs to /processes.
 */
async function requestProcess(url: string, failureMessage: string) {
  error.value = null
  try {
    const response = await authFetch(url)
    if (!response.ok) throw new Error("Network response was not ok")
    flashUpdateState('requested')
  } catch (err: any) {
    console.error(err)
    error.value = failureMessage
    flashUpdateState('failed')
  }
}

function flashUpdateState(state: 'requested' | 'failed') {
  updateState.value = state
  clearTimeout(updateStateTimer)
  updateStateTimer = setTimeout(() => (updateState.value = 'idle'), state === 'failed' ? 2500 : 1500)
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

/**
 * The transcript as it was stored, pages in order behind a marker line. page.transcript is the
 * unrendered text, so the named entity syntax (<T : tag> and friends) is copied verbatim and none
 * of renderNamedEntities' HTML is involved — that difference is what "Copy as MD" will cover.
 * Pages still being OCR'd have no text yet and are skipped rather than pasted as a bare marker;
 * the marker numbering makes the gap visible.
 */
function rawTranscript(): string {
  return transcript.value.pages
      .filter(page => page.transcript && page.transcript.trim().length > 0)
      //pageNumber is 0-based server side, displayed +1 everywhere else in this view
      .map(page => `--- page ${page.pageNumber + 1} ---\n${page.transcript}`)
      .join('\n\n')
}

async function copyRaw() {
  await copyToClipboard(rawTranscript())
}

function copyAsMarkdown() {
  //TODO render the markdown flavour: named entity syntax to markdown, headings from the h2/h3 verbs
}

async function copyToClipboard(text: string) {
  try {
    //undefined outside a secure context (a plain-http tunnel), which throws here rather than
    //rejecting — either way it lands in the catch and shows as a failed copy
    await navigator.clipboard.writeText(text)
    flashCopyState('copied')
  } catch (err) {
    console.error('Copy failed:', err)
    flashCopyState('failed')
  }
}

//the button is the only feedback channel: this view's error ref is never rendered
function flashCopyState(state: 'copied' | 'failed') {
  copyState.value = state
  clearTimeout(copyStateTimer)
  copyStateTimer = setTimeout(() => (copyState.value = 'idle'), state === 'failed' ? 2500 : 1500)
}

onUnmounted(() => {
  clearTimeout(copyStateTimer)
  clearTimeout(updateStateTimer)
})

function agent(fileId) {
  router.push({ name: 'agent', params: { fileId } })
}

function togglePageImage(pageNumber: number) {
  pageShowImages.value[pageNumber] = !pageShowImages.value[pageNumber]
}

function toggleAllImages() {
  allImagesShown.value = !allImagesShown.value
  transcript.value.pages.forEach(page => {
    pageShowImages.value[page.pageNumber] = allImagesShown.value
  })
}

//the Save/Reset buttons live in the page header, which this view owns, while the text being edited
//lives in the page component — so the header needs a handle on the page it is acting for. Keyed by
//page number rather than collected as an array: a v-for ref array's order is not guaranteed to
//match the rendered order. Vue calls the function ref with null on unmount, hence the delete.
const pageRefs = ref<Record<number, { save: () => void; reset: () => void } | null>>({})

function setPageRef(pageNumber: number, el: any) {
  if (el) pageRefs.value[pageNumber] = el
  else delete pageRefs.value[pageNumber]
}

function handleEditRequest(pageNumber: number, isClosing: boolean) {
  if (isClosing) {
    activeEditPageNumber.value = null
  } else {
    activeEditPageNumber.value = pageNumber
  }
}

let expectedReadyPages = 0
const pagesReadyCount = ref(0)

async function scrollToHashAnchor() {
  if (!route.hash) return
  await nextTick()
  document.getElementById(route.hash.slice(1))?.scrollIntoView({ block: 'start' })
}

function handlePageReady() {
  pagesReadyCount.value++
  if (pagesReadyCount.value >= expectedReadyPages) {
    scrollToHashAnchor()
  }
}

onMounted(async () => {
  await fetchTranscript()
  expectedReadyPages = transcript.value.pages.filter(p => p.pageDiagram !== PageDiagram.inline).length
  if (expectedReadyPages === 0) {
    scrollToHashAnchor()
  }
});

</script>

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

.transcript-header .action-row {
  margin-left: auto;
  flex-wrap: wrap;
  justify-content: flex-end;
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

.page-edit-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-left: 1rem;
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-left: auto;
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
