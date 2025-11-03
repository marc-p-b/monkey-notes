<template>

  <div class="home-wrapper">
    <div v-if="loading">Loading...</div>

    <div v-else>
      <h1>{{transcript.title}}</h1>

      <p>transcripted : {{formatDate(transcript.transcripted_at)}}</p>
      <p>documented : {{formatDate(transcript.documented_at)}}</p>
      <p>discovered : {{formatDate(transcript.discovered_at)}}</p>
      <p>pages : {{transcript.pages.length}}</p>
      <p>version : {{transcript.version}}</p>

      <a href="#" @click.prevent="agent(transcript.fileId)">agent</a> -
      <a href="#" @click.prevent="updateTranscript(transcript.fileId)">update</a> -
      <a href="#" @click.prevent="downloadFile(transcript.fileId)">get pdf</a>

      <div v-for="page in transcript.pages">
        <TranscriptPage :page="page"/>
      </div>
    </div>
  </div>


</template>

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
}

const transcript = ref<DtoTranscript>(null)


// ✅ utility function for consistent date formatting
function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date)
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
