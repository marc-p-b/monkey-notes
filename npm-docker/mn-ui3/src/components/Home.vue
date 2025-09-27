<template>
  <div>
    <h2>Recent transcripts</h2>

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

  <div>
    <h2>Folders</h2>

    <ul>
      <li
          v-for="node in nodes" :key="node.dtoFile.fileId"
      >
        {{ node.name }}
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

interface DtoFile {
  fileId: string;
  name: string;
}

interface FileNode {
  dtoTranscript: DtoTranscript
  dtoFile: DtoFile
  name: string
  folder: boolean
}

const transcripts = ref<DtoTranscript[]>([])
const nodes = ref<FileNode[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function fetchRecentTranscripts() {
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

async function fetchRootFolders() {

  try {
    const response = await authFetch("http://localhost:8080/transcript/folder/list");
    if (!response.ok) throw new Error("Network response was not ok");

    nodes.value = await response.json()

    //console.log(response.json())


  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }

}


onMounted(() => {
  fetchRecentTranscripts();
  fetchRootFolders();
});

</script>
