<template>
  <div v-if="error" class="empty-state">
    <i class="pi pi-exclamation-triangle"></i>
    {{ error }}
  </div>

  <div v-else-if="!loading && sortedNodes.length === 0" class="empty-state">
    <i class="pi pi-info-circle"></i>
    No documents yet — notes synced from your tablet appear here once processed
  </div>

  <ul v-else class="tree-root">
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
//starts true because a root fetch always fires on mount: without it the empty state renders for
//one frame before the first response lands
const loading = ref(true)

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
  const isRoot = node === null
  emit("loading-status", true);
  if (isRoot) {
    loading.value = true
    error.value = null
  }
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
    //only a failed root fetch replaces the panel — a subfolder that fails to expand must not
    //blank out the tree that is already on screen
    if (isRoot) {
      error.value = "Failed to load documents.";
    }
  } finally {
    if (isRoot) {
      loading.value = false
    }
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

/* same declaration as QuickNotesView.vue / NamedEntitiesView.vue, which each keep their own copy */
.empty-state {
  color: var(--p-surface-400);
  font-size: 0.875rem;
  font-style: italic;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>