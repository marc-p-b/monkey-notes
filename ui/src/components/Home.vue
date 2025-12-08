<template>
  <div class="home-wrapper">

    <h2>Recent transcripts</h2>
    <div v-if="loading">Loading...</div>
      <ul v-else>
        <li v-for="transcript in transcripts" :key="transcript.fileId">
          <a href="#" @click.prevent="clickedTranscript(transcript.transcript.fileId)">
            {{ transcript.transcript.name }}
          </a>
        </li>
      </ul>
  <h2>Transcripts</h2>
    <TreeView
        @loading-status="loadingStatus"
    ></TreeView>

  </div>

</template>

<script lang="ts" setup>
import TreeView from "@/components/TreeView.vue";
import { ref, onMounted } from "vue";
import {authFetch} from "@/requests";
import { useRouter } from 'vue-router'
const router = useRouter()
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

interface DtoTranscript {
  fileId: string;
  name: string;
}

const transcripts = ref<DtoTranscript[]>([])
const error = ref<string | null>(null)

let recentLoading: boolean = false
let foldersLoading: boolean = false

async function fetchRecentTranscripts() {
  recentLoading = true
  error.value = null;
  try {
    const response = await authFetch("transcript/recent");
    if (!response.ok) throw new Error("Network response was not ok");
    transcripts.value = await response.json();

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    recentLoading = false
    homeLoading()
  }
}

function clickedTranscript(fileId) {
  router.push({ name: 'transcript', params: { fileId } })
}

function loadingStatus(status: boolean) {
  if(status) {
    foldersLoading = true
  } else {
    foldersLoading = false
  }
  homeLoading()
}

function homeLoading() {
  if(foldersLoading || recentLoading) {
    store.setLoading(true)
  } else {
    store.setLoading(false)
  }
}

onMounted(() => {
  fetchRecentTranscripts();
});

</script>

<style>

</style>