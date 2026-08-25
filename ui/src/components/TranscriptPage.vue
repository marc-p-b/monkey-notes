<template>
  <span :id="'pageNumber' + page.pageNumber" />


  <div v-if="page.pageDiagram == PageDiagram.full">

    <img v-if="imgSrc" :src="imgSrc" alt="preview" class="preview-img"/>
    <p v-else>Loading source image...</p>

  </div>

  <div v-else-if="editMode===false && showImages" class="flex-row view-image-row">
    <div class="view-left">
      <p v-html="text" @click.prevent="switchEdit(page)"></p>
    </div>
    <div class="view-right">
      <img v-if="imgSrc" :src="imgSrc" alt="preview" class="preview-img view-preview-img"/>
      <p v-else class="image-loading">Loading image...</p>
    </div>
  </div>
  <p v-else-if="editMode===false" v-html="text" @click.prevent="switchEdit(page)"></p>
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

    <div class="buttons">
      <Button @click.prevent="save" label="save" />
      <Button @click.prevent="closeEdit" label="close" />
    </div>
  </div>
  <div class="page-footer">
    <Button @click.prevent="updatePage(page)" icon="pi pi-refresh" text severity="secondary" size="small" v-tooltip.top="'Re-transcribe page'" />
    <Badge v-if="page.deltas > 0" :value="page.deltas + (page.deltas === 1 ? ' delta' : ' deltas')" severity="secondary" />
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
import {ref, defineProps, defineEmits, onMounted, watch} from "vue";
import {authFetch} from "@/requests";
import {renderNamedEntities} from "@/utils/namedEntityRender";

import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

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

const switchEdit = async (page) => {
  if(store.transcript_edit_mode === false) {
    return
  }
  emit('requestEdit', page.pageNumber, false)
  downloadImage(page)
  editMode.value = true
}
const closeEdit = async () => {
  emit('requestEdit', props.page.pageNumber, true)
  editMode.value = false
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

.buttons {
  display: flex;
  gap: 0.5rem;
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