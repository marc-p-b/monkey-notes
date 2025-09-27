<template>
  <div>
    <h2>Transcripts</h2>

    <div v-if="loading" >Loading...</div>
    <div v-else-if="error" >{{ error }}</div>

    <ul v-else >
      <li
          v-for="transcript in transcripts" :key="transcript.transcript.fileId"
      >
        {{ transcript.transcript.name }}
      </li>
    </ul>
  </div>


</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { authFetch } from "@/requests.ts";

interface DtoTranscript {
  fileId: string;
  name: string;
}

const transcripts = ref<DtoTranscript[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

async function fetchTranscripts() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("http://localhost:8080/transcript/recent");
    if (!response.ok) throw new Error("Network response was not ok");
    transcripts.value = await response.json();
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchTranscripts();
});

</script>
