<template>

  <div class="home-wrapper">
    <div v-if="loading">Loading...</div>

    <div v-else>
      <h1>{{transcript.title}}</h1>
        <div class="card">
          <Tabs value="0">
            <TabList>
              <Tab value="0">Properties</Tab>
              <Tab value="1">Tags</Tab>
              <Tab value="2">TOC</Tab>
            </TabList>
            <TabPanels>
              <TabPanel value="0">
                <p class="m-0">
                  <ul class="tags">
                    <li>transcripted : {{formatDate(transcript.transcripted_at)}}</li>
                    <li>documented : {{formatDate(transcript.documented_at)}}</li>
                    <li>discovered : {{formatDate(transcript.discovered_at)}}</li>
                    <li>pages : {{transcript.pages.length}}</li>
                    <li>version : {{transcript.version}}</li>
                  </ul>
                  <a href="#" @click.prevent="agent(transcript.fileId)">agent</a> -
                  <a href="#" @click.prevent="updateTranscript(transcript.fileId)">update</a> -
                  <a href="#" @click.prevent="downloadFile(transcript.fileId)">get pdf</a>
                </p>
              </TabPanel>
              <TabPanel value="1">
                <p class="m-0">
                  <ul class="tags">
                    <p v-if="transcript.tags.length === 0">No tags</p>
                    <li v-for="tag in transcript.tags">
                      {{tag.value}} (p. {{tag.pageNumber+1}})
                    </li>
                  </ul>
                </p>
              </TabPanel>
              <TabPanel value="2">
                <p class="m-0">
                  <p v-if="transcript.toc.length === 0">No toc</p>
                  <ul class="toc">
                    <li v-for="item in transcript.toc"
                        :style="{ marginLeft: getIndent(item.verb) + 'px' }">
                      {{item.value}} <a :href="'#' + item.uuid"><i class="pi pi-link"></i></a>
                    </li>
                  </ul>
                </p>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </div>


      <div v-for="page in transcript.pages">
        <TranscriptPage :page="page"/>
      </div>
    </div>
  </div>


</template>

<style scoped>

.tags li {
  display: inline;         /* ✅ inline display */
  margin-right: 1rem;      /* spacing between items */
}

.tags {
  list-style: none; /* ✅ removes dots */
  padding-left: 0;  /* ✅ removes default left padding */
  margin: 0;
}

.toc {
  list-style: none; /* ✅ removes dots */
  padding-left: 0;  /* ✅ removes default left padding */

}

.toc li {
  line-height: 1.6;
  list-style: none;

}
.toc-title {
  font-weight: 500;
}
.toc-page {
  color: #666;
  font-size: 0.9em;
}
</style>

<script lang="ts" setup>
import { ref, onMounted, computed } from "vue";
import { authFetch } from "@/requests.ts";
import { defineProps } from 'vue'
import TranscriptPage from "./TranscriptPage.vue";
import { useRouter } from 'vue-router'
const router = useRouter()

const props = defineProps<{ fileId: string }>()

const loading = ref(true)
const error = ref<string | null>(null)

const transcript = ref<DtoTranscript>(null)

interface DtoTranscript {
  username: string
  fileId: string
  name: string
  transcripted_at: string // use string if coming from JSON (ISO date format)
  documented_at: string
  discovered_at: string
  pageCount: number
  version: number
  pages: Page[]
  title: string
  tags: NamedEntity[]
  toc: NamedEntity[]
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

// ✅ utility function for consistent date formatting
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
    case 'h3': return 20
    case 'h4': return 40
    case 'h5': return 60
    case 'h6': return 80
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

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`)
    }

    // Convert to Blob (binary data)
    const blob = await response.blob()

    // Extract filename from Content-Disposition header (if provided)
    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = 'downloaded-file'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="(.+)"/)
      if (match) fileName = match[1]
    }

    // Create a temporary link and trigger download
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

onMounted(() => {
  fetchTranscript();
});

</script>
