<template>

  <h2>Recent transcripts</h2>
  <div v-if="loading">Loading...</div>
  <ul v-else>
    <li v-for="transcript in transcripts" :key="transcript.fileId">
      <a href="#" @click.prevent="clickedTranscript(transcript.transcript.fileId)">
        📄 Transcript {{ transcript.transcript.name }}
      </a>
    </li>
  </ul>

  <h2>Transcripts</h2>
  <TreeView />

</template>

<script lang="ts" setup>
import TreeView from "@/components/TreeView.vue";
import { ref, onMounted } from "vue";
import {authFetch} from "@/requests";
import { useRouter } from 'vue-router'
const router = useRouter()

interface DtoTranscript {
  fileId: string;
  name: string;
}

const transcripts = ref<DtoTranscript[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function fetchRecentTranscripts() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("transcript/recent");
    if (!response.ok) throw new Error("Network response was not ok");
    transcripts.value = await response.json();

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }
}

function clickedTranscript(fileId) {
  router.push({ name: 'transcript', params: { fileId } })
}

onMounted(() => {
  fetchRecentTranscripts();
});

</script>
