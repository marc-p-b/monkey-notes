<template>

  <div v-if="loading">Loading...</div>

  <div v-else>
    <h2>{{transcript.title}}</h2>

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
    const response = await authFetch("http://localhost:8080/transcript/" + props.fileId);
    if (!response.ok) throw new Error("Network response was not ok");
    transcript.value = await response.json();

    //console.log(response.json())

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchTranscript();
});

</script>
