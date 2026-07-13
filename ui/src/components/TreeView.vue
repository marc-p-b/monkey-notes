<template>
  <ul class="tree-root">
    <TreeNode
        v-for="node in sortedNodes"
        :key="node.dtoFile.fileId"
        :node="node"
        :select-mode="selectMode"
        :order-by="orderBy"
        :order-dir="orderDir"
        @folder-clicked="handleFolderClick"
        @transcript-clicked="handleTranscriptClick"
    />
  </ul>
</template>

<script setup lang="ts">
import { authFetch } from "@/requests";
import TreeNode from "./TreeNode.vue";
import { ref, computed, onMounted, defineEmits } from "vue";
import { useRouter } from 'vue-router'
import { sortNodes } from "@/utils/treeSort";
const router = useRouter()

interface Node {
  name: string;
  folder: boolean;
  dtoFile: {
    fileId: string | number;
    discovered_at?: string;
  };
  children?: Node[];
}

const props = withDefaults(defineProps<{
  selectMode?: boolean;
  orderBy?: 'name' | 'date';
  orderDir?: 'asc' | 'desc';
}>(), {
  selectMode: false,
  orderBy: 'name',
  orderDir: 'asc',
});

const nodes = ref<Node[]>([])
const error = ref<string | null>(null)

const sortedNodes = computed(() => sortNodes(nodes.value, props.orderBy, props.orderDir))

const emit = defineEmits<{
  (e: "loading-status", status: boolean): void;
}>();

const handleFolderClick = async (node: Node) => {
  await fetchFolder(node)
};

const handleTranscriptClick = (fileId: string | number) => {
  router.push({ name: 'transcript', params: { fileId } })
};

async function fetchFolder(node: Node | null) {
  emit("loading-status", true);
  try {
    const url = node ? "transcript/folder/list/" + node.dtoFile.fileId : "transcript/folder/list"
    const response = await authFetch(url);
    if (!response.ok) throw new Error("Network response was not ok");
    if (node) {
      node.children = await response.json();
    } else {
      nodes.value = await response.json()
    }
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    emit("loading-status", false);
  }
}

onMounted(() => {
  fetchFolder(null);
});
</script>

<style scoped>
.tree-root {
  list-style: none;
  padding: 0;
  margin: 0;
}
</style>