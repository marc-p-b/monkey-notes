<template>

  <div v-if="loading">Loading...</div>

  <div v-else>
    <h2>{{transcript.title}}</h2>
    <a @click.prevent="agent(transcript.fileId)">agent</a>

<!--    '/transcript/update/' + fileId;-->
<!--    '/transcript/pdf/' + fileId;-->

    <div v-for="page in transcript.pages">
      <TranscriptPage :page="page"/>
    </div>
  </div>


</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
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

function agent(fileId) {
  router.push({ name: 'agent', params: { fileId } })
}

onMounted(() => {
  fetchTranscript();
});

</script>
