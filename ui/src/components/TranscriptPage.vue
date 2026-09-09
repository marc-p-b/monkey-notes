<template>
  <span :id="'pageNumber' + page.pageNumber" />


  <div v-if="page.pageDiagram == PageDiagram.full">

    <img v-if="imgSrc" :src="imgSrc" alt="preview" class="preview-img"/>
    <p v-else>Loading source image...</p>

  </div>

  <div v-else-if="editMode===false && showImages" class="flex-row view-image-row">
    <div class="view-left">
      <p v-html="text" @dblclick.prevent="switchEdit(page)"></p>
    </div>
    <div class="view-right">
      <img v-if="imgSrc" :src="imgSrc" alt="preview" class="preview-img view-preview-img"/>
      <p v-else class="image-loading">Loading image...</p>
    </div>
  </div>
  <p v-else-if="editMode===false" v-html="text" @dblclick.prevent="switchEdit(page)"></p>
  <div v-else class="edit-container">
    <div class="flex-row">
      <div class="left">
      <Textarea
          v-model="textEdit"
          auto-resize
          class="w-full"
      />
      </div>
      <div class="right">
        <img v-if="imgSrc" :src="imgSrc" alt="preview" class="preview-img"/>
        <p v-else>Loading source image...</p>
      </div>

    </div>
  </div>
  <div class="page-footer">
    <Button @click.prevent="updatePage(page)" icon="pi pi-refresh" text severity="secondary" size="small" v-tooltip.top="'Re-transcribe page'" />
    <div v-if="page.deltas > 0" ref="deltaWrapper" class="delta-wrapper">
      <Badge :value="page.deltas + (page.deltas === 1 ? ' delta' : ' deltas')" severity="secondary" class="delta-badge" @click="toggleDeltas" />
      <!-- opens upward: the badge sits in the page footer, so a downward panel would cover the next
           page's header instead of this page's own content -->
      <div v-if="deltasOpen" class="delta-popup">
        <div v-if="deltasLoading" class="delta-empty">Loading versions...</div>
        <div v-else-if="deltaVersions.length === 0" class="delta-empty">No stored version</div>
        <!-- v-for lives on its own element: Vue 3 resolves v-if before v-for on a single element,
             which makes the pair a warning even when, as here, the alias isn't read by the branch -->
        <template v-else>
        <div v-for="(delta, index) in deltaVersions" :key="delta.version" class="delta-entry">
          <span class="delta-version">v{{ delta.version }}</span>
          <span class="delta-date">{{ formatDateTime(delta.createdAt) }}</span>
          <Button
              v-if="index === deltaVersions.length - 1"
              icon="pi pi-trash"
              text
              size="small"
              severity="secondary"
              class="delta-delete"
              v-tooltip.top="'Delete this version'"
          />
        </div>
        </template>
      </div>
    </div>
    <div class="footer-stats">
      <span v-if="showStats" class="stats-info">
        <span class="stat-model">{{ page.aiModel }}</span>
        <span class="stat-tokens" v-tooltip.top="'Prompt tokens'"><i class="pi pi-arrow-up"></i> {{ page.tokensPrompt }}</span>
        <span class="stat-tokens" v-tooltip.top="'Response tokens'"><i class="pi pi-arrow-down"></i> {{ page.tokensResponse }}</span>
      </span>
      <Button @click.prevent="showStats = !showStats" icon="pi pi-info-circle" text severity="secondary" size="small" v-tooltip.top="'Show OCR stats'" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import {ref, defineProps, defineEmits, onMounted, onUnmounted, watch} from "vue";
import {authFetch} from "@/requests";
import {renderNamedEntities} from "@/utils/namedEntityRender";
import {formatDateTime} from "@/utils/documentDate";


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

enum PageDiagram {
  none = 'none',
  full = 'full',
  inline = 'inline'
}


interface Page {
  fileId: string
  username: string
  pageNumber: number
  transcript: string
  transcriptTook: number
  tokensPrompt: number
  tokensResponse: number
  version: number
  aiModel: string
  imageUrl: string
  completed: boolean
  listNamedEntities: NamedEntity[]
  cols: number
  rows: number
  deltas: number
  pageDiagram: PageDiagram
  diagramTitle: string
}

const imgSrc = ref(null)
const diagramImgSrc = ref(null)
const props = defineProps<{
  page: Page
  nextPage: Page | null
  activeEditPageNumber: number | null
  showImages: boolean
}>()

const emit = defineEmits<{
  requestEdit: [pageNumber: number, isClosing: boolean]
  pageReady: []
}>()

const text = ref()
const textEdit = ref()
const loading = ref(true)
const error = ref<string | null>(null)
const editMode = ref(false)
const showStats = ref(false)
let transcript = props.page.transcript;
textEdit.value = transcript

async function updatePage(page) {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("transcript/update/" + page.fileId + '/' + page.pageNumber);
    if (!response.ok) throw new Error("Network response was not ok");
    //console.log(response)
  } catch (err: any) {
    //console.error(err);
    error.value = "Failed to update transcript page.";
  } finally {
    loading.value = false;
  }
}

//reached from the page's own pencil button and from a double click on the text. A single click is
//deliberately not a trigger: the rendered text carries live checkboxes and links, and with no
//view-wide edit mode left to gate it, one stray click would swap the page out from under the reader
const switchEdit = async (page) => {
  emit('requestEdit', page.pageNumber, false)
  downloadImage(page)
  editMode.value = true
}

async function downloadImage(page) {
  const path = "image/" + page.username + "/" + page.fileId + "/" + page.pageNumber
  const res = await authFetch(path)
  const blob = await res.blob()

  if(imgSrc.value) {
    URL.revokeObjectURL(imgSrc.value)
  }
  imgSrc.value = URL.createObjectURL(blob)
}

async function downloadNextPageImage() {
  if (!props.nextPage) return
  const path = "image/" + props.nextPage.username + "/" + props.nextPage.fileId + "/" + props.nextPage.pageNumber
  const res = await authFetch(path)
  const blob = await res.blob()

  if (diagramImgSrc.value) {
    URL.revokeObjectURL(diagramImgSrc.value)
  }
  diagramImgSrc.value = URL.createObjectURL(blob)
}

const save = async () => {
  const fileId = props.page.fileId
  const pageNumber = props.page.pageNumber
  emit('requestEdit', props.page.pageNumber, true)
  editMode.value = false;

  try {
    const response = await authFetch("transcript/edit/" + fileId + "/" + pageNumber, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: textEdit.value,
    });

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }
  } catch (err: any) {
    error.value = err.message || "Something went wrong.";
  } finally {
    loading.value = false;
    transcript = textEdit.value;
    await loadPage();
  }
}

interface PageDiff {
  version: number
  createdAt: string
}

const deltaWrapper = ref<HTMLElement | null>(null)
const deltasOpen = ref(false)
const deltasLoading = ref(false)
const deltaVersions = ref<PageDiff[]>([])

/**
 * The page's edit history — one entry per stored diff. Not the same number as page.deltas, which
 * counts the hunks inside the diff for the page's current version, so the list can be shorter or
 * longer than the badge. Refetched on every open rather than cached: saving an edit writes a new
 * row, and this panel is the only place that would show it.
 */
async function fetchDeltaVersions() {
  deltasLoading.value = true
  try {
    const response = await authFetch("transcript/deltas/" + props.page.fileId + "/" + props.page.pageNumber)
    if (!response.ok) throw new Error("Network response was not ok")
    deltaVersions.value = await response.json()
  } catch (err: any) {
    console.error(err)
    deltaVersions.value = []
  } finally {
    deltasLoading.value = false
  }
}

function toggleDeltas() {
  deltasOpen.value = !deltasOpen.value
  if (deltasOpen.value) fetchDeltaVersions()
}

//a dropdown the user can't dismiss by clicking away would be a trap; unlike the editor, nothing is
//committed here, so closing on an outside click costs nothing
function onDocumentClick(event: MouseEvent) {
  if (!deltasOpen.value) return
  if (!deltaWrapper.value?.contains(event.target as Node)) deltasOpen.value = false
}

//bound only while the panel is open, and post-flush so the click that opened it is already over
watch(deltasOpen, (open) => {
  if (open) document.addEventListener('click', onDocumentClick)
  else document.removeEventListener('click', onDocumentClick)
}, { flush: 'post' })

onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick)
})

//discards the edits and puts the last stored text back, staying in edit mode — with no cancel
//button left, this is how you get out of a change you don't want before it is saved
const reset = () => {
  textEdit.value = transcript
}

//the Save and Reset buttons live in the page header, which TranscriptView owns
defineExpose({ save, reset })

const loadPage = async () => {
  if(props.page.pageDiagram == PageDiagram.full) {
    await downloadImage(props.page)
  } else {
    const hasDiagramNextPage = props.page.listNamedEntities.some(ne => ne.verb == 'diagramNextPage')
    if (hasDiagramNextPage) {
      await downloadNextPageImage()
    }
  }


  // `transcript` deliberately stays the raw text: the renderer is pure, so re-running loadPage()
  // (which save() does) can no longer double-render an already-rendered string.
  text.value = renderNamedEntities(transcript, props.page.listNamedEntities, {
    diagramImageSrc: diagramImgSrc.value
  })
}

//use store instead ?
watch(() => props.activeEditPageNumber, async (newActivePageNumber) => {
  if (newActivePageNumber === props.page.pageNumber && !editMode.value) {
    await downloadImage(props.page)
    editMode.value = true
  } else if (newActivePageNumber !== props.page.pageNumber && editMode.value) {
    editMode.value = false
  }
})

//use store instead ?
watch(() => props.showImages, async (val) => {
  if (val && !imgSrc.value && !props.page.diagram) {
    await downloadImage(props.page)
  }
})

onMounted(async () => {
  try {
    await loadPage()
  } finally {
    emit('pageReady')
  }
});

</script>

<style>

.edit-container {
  display: flex;
  flex-direction: column;  /* stack flex-row and buttons */
  gap: 1rem;
}

.flex-row {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.left {
  flex: 1;
}

.right {
  flex: 1;
}

.right .preview-img {
  width: 100%;
}

.preview-img {
  width: 80%;
  height: auto;
}

.diagram-inline-img {
  display: block;
  width: 60%;
  margin-top: 0.5rem;
}

.view-image-row {
  align-items: flex-start;
}

.view-left {
  flex: 1;
}

.view-right {
  flex: 1;
}

.view-preview-img {
  width: 100%;
}

.image-loading {
  color: var(--p-surface-400);
  font-size: 0.875rem;
  font-style: italic;
}

.delta-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.delta-badge {
  cursor: pointer;
}

/* anchored to the badge and opening upward. .page-card clips its children, so the panel is capped
   and scrolls rather than being cut off on a page with a long edit history */
.delta-popup {
  position: absolute;
  bottom: calc(100% + 0.35rem);
  left: 0;
  z-index: 10;
  min-width: 14rem;
  max-height: 13rem;
  overflow-y: auto;
  padding: 0.25rem;
  background-color: var(--p-content-background);
  border: 1px solid var(--p-surface-200);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.delta-entry {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.4rem;
  border-radius: 4px;
  white-space: nowrap;
}

.delta-entry:hover {
  background-color: var(--p-content-hover-background);
}

.delta-version {
  font-size: 0.75rem;
  font-weight: 600;
}

.delta-date {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.delta-delete {
  margin-left: auto;
  width: 1.5rem !important;
  height: 1.5rem !important;
  padding: 0 !important;
}

.delta-empty {
  padding: 0.35rem 0.4rem;
  font-size: 0.75rem;
  font-style: italic;
  color: var(--p-surface-400);
}

.page-footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--p-surface-100);
}

.footer-stats {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-left: auto;
}

.stats-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--p-surface-400);
}

.stat-model {
  font-style: italic;
}

.stat-tokens {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

</style>