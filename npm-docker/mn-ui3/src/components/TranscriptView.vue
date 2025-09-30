<template>

  <div v-if="loading">Loading...</div>

  <div v-else>
    <h2>{{transcript.title}}</h2>

    <div v-for="page in transcript.pages">
      page {{page.pageNumber+1}}
      <p>{{page.transcript}}</p>
    </div>
  </div>


</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { authFetch } from "@/requests.ts";
import { defineProps } from 'vue'

const props = defineProps<{ fileId: string }>()

const loading = ref(true)
const error = ref<string | null>(null)

interface DtoTranscript {
  username: string
  fileId: string
  name: string

  // last update (images -> transcript)
  transcripted_at: string // use string if coming from JSON (ISO date format)

  // date from title (regex)
  documented_at: string

  // file first discovery // todo replace with drive date ?
  discovered_at: string

  pageCount: number
  version: number

  pages: DtoTranscriptPage[]

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
