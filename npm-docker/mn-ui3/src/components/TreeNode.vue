

<template>
  <li>
    <template v-if="node.folder">
      <a href="#" @click.prevent="clickedNodeFolder(node.dtoFile.fileId)">
        📁 Folder {{ node.name }}
      </a>
      <ul v-if="node.children && node.children.length">
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
      <a href="#" @click.prevent="clickedTranscript(node.dtoFile.fileId)">
        📄 Transcript {{ node.name }} - {{node.dtoFile.fileId}}
      </a>
    </template>
  </li>
</template>


<script setup lang="ts">
import TreeNode from "./TreeNode.vue";
import { defineProps, defineEmits } from "vue";

interface Node {
  name: string;
  folder: boolean;
  dtoFile: {
    fileId: string | number;
  };
  children?: Node[]; // recursion
}

const props = defineProps<{
  node: Node;
}>();

const emit = defineEmits<{
  (e: "folder-clicked", fileId: string | number): void;
  (e: "transcript-clicked", fileId: string | number): void;
}>();

const clickedNodeFolder = (fileId: string | number) => {
  emit("folder-clicked", fileId);
};

const clickedTranscript = (fileId: string | number) => {
  emit("transcript-clicked", fileId);
};
</script>