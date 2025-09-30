<template>
  <div v-if="loading">Loading...</div>
  <ul v-else>
    <TreeNode
        v-for="node in nodes"
        :key="node.dtoFile.fileId"
        :node="node"
        @folder-clicked="handleFolderClick(node)"
        @transcript-clicked="handleTranscriptClick"
    />
  </ul>
</template>

<script setup lang="ts">
import { authFetch } from "@/requests.ts";
import TreeNode from "./TreeNode.vue";
import {ref, onMounted} from "vue";
import { useRouter } from 'vue-router'
const router = useRouter()

interface Node {
  name: string;
  folder: boolean;
  dtoFile: {
    fileId: string | number;
  };
  children?: Node[];
}

const nodes = ref<Node[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const handleFolderClick = async (node: Node) => {
  fetchFolder(node.dtoFile.fileId, node)
};

const handleTranscriptClick = (fileId: string | number) => {
  //console.log("Transcript clicked:", fileId);
  router.push({ name: 'transcript', params: { fileId } })
};

async function fetchFolder(fileId : string, node?: Node) {
  try {
    let url = ""
    if(fileId.length === 0) {
      url = "http://localhost:8080/transcript/folder/list"
    } else {
      url = "http://localhost:8080/transcript/folder/list/" + fileId
    }
    const response = await authFetch(url);

    if (!response.ok) throw new Error("Network response was not ok");

    if(fileId.length === 0) {
      nodes.value = await response.json()
    } else {
      node.children = await response.json();
    }
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchFolder("");
});

</script>



