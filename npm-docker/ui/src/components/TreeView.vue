<template>
  <div v-if="loading">Loading...</div>
  <ul v-else>
    <TreeNode
        v-for="node in nodes"
        :key="node.dtoFile.fileId"
        :node="node"
        @folder-clicked="handleFolderClick"
        @transcript-clicked="handleTranscriptClick"
    />
  </ul>
</template>

<script setup lang="ts">
import {authFetch} from "@/requests";
import TreeNode from "./TreeNode.vue";
import {ref, onMounted, defineEmits} from "vue";
import { useRouter } from 'vue-router'
const router = useRouter()
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

interface Node {
  name: string;
  folder: boolean;
  dtoFile: {
    fileId: string | number;
  };
  children?: Node[];
}

const nodes = ref<Node[]>([])
const error = ref<string | null>(null)

const emit = defineEmits<{
  (e: "loading-status", status: boolean): void;
}>();

// const endLoading = () => {
//   emit("loading-end", true);
// };

const handleFolderClick = async (node: Node) => {
  await fetchFolder(node)
};

const handleTranscriptClick = (fileId: string | number) => {
  router.push({ name: 'transcript', params: { fileId } })
};

async function fetchFolder(node: Node) {
  emit("loading-status", true);
  try {
    let url = ""
    if(node) {
      url = "transcript/folder/list/" + node.dtoFile.fileId
    } else {
      url = "transcript/folder/list"
    }
    const response = await authFetch(url);
    if (!response.ok) throw new Error("Network response was not ok");
    if(node) {
      node.children = await response.json();
    } else {
      nodes.value = await response.json()
    }
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    //store.setLoading(false)
    emit("loading-status", false);
  }
}

onMounted(() => {
  fetchFolder("");
});

</script>



