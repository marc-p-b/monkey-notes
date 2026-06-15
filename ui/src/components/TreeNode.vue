<template>
  <li class="tree-item">
    <template v-if="node.folder">
      <div class="tree-row folder-row" @click="toggleFolder">
        <i :class="['pi', expanded ? 'pi-chevron-down' : 'pi-chevron-right', 'chevron']"></i>
        <i class="pi pi-folder folder-icon"></i>
        <span class="node-name">{{ node.name }}</span>
        <Button
          icon="pi pi-refresh"
          size="small"
          text
          severity="secondary"
          class="update-btn"
          @click.stop="updateFolder(node.dtoFile.fileId)"
        />
      </div>
      <ul v-if="expanded && node.children && node.children.length" class="children-list">
        <TreeNode
            v-for="child in node.children"
            :key="child.dtoFile.fileId"
            :node="child"
            @folder-clicked="emit('folder-clicked', $event)"
            @transcript-clicked="emit('transcript-clicked', $event)"
        />
      </ul>
    </template>

    <template v-else>
      <div class="tree-row file-row" @click="emit('transcript-clicked', node.dtoFile.fileId)">
        <span class="chevron-space"></span>
        <i class="pi pi-file-edit file-icon"></i>
        <span class="node-name">{{ node.name }}</span>
      </div>
    </template>
  </li>
</template>

<script setup lang="ts">
import TreeNode from "./TreeNode.vue";
import { authFetch } from "@/requests";
import { ref } from "vue";

const error = ref<string | null>(null)
const expanded = ref(false)

interface Node {
  name: string;
  folder: boolean;
  dtoFile: {
    fileId: string | number;
  };
  children?: Node[];
}

const props = defineProps<{
  node: Node;
}>();

const emit = defineEmits<{
  (e: "folder-clicked", node: Node): void;
  (e: "transcript-clicked", fileId: string | number): void;
}>();

function toggleFolder() {
  if (expanded.value) {
    expanded.value = false;
  } else {
    expanded.value = true;
    if (!props.node.children || props.node.children.length === 0) {
      emit("folder-clicked", props.node);
    }
  }
}

async function updateFolder(fileId) {
  error.value = null;
  try {
    const response = await authFetch("transcript/folder/update/" + fileId);
    if (!response.ok) throw new Error("Network response was not ok");
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to update folder.";
  }
}
</script>

<style scoped>
.tree-item {
  list-style: none;
}

.tree-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: background 0.12s;
}

.tree-row:hover {
  background-color: var(--p-content-hover-background);
}

.chevron {
  font-size: 0.65rem;
  color: var(--p-text-muted-color);
  width: 10px;
  flex-shrink: 0;
}

.chevron-space {
  width: 10px;
  flex-shrink: 0;
}

.folder-icon {
  font-size: 0.9rem;
  color: #f59e0b;
  flex-shrink: 0;
}

.file-icon {
  font-size: 0.9rem;
  color: var(--p-text-muted-color);
  flex-shrink: 0;
}

.node-name {
  flex: 1;
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.update-btn {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.12s;
  width: 1.5rem !important;
  height: 1.5rem !important;
  padding: 0 !important;
}

.folder-row:hover .update-btn {
  opacity: 1;
}

.children-list {
  margin-left: 1.25rem;
  padding: 0;
}
</style>