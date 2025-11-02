

<template>
  <li>
    <template v-if="node.folder">
      <a href="#" @click.prevent="clickedNodeFolder(node)">
        📁 {{ node.name }}
      </a> - <a @click.prevent="updateFolder(node.dtoFile.fileId)">update</a>
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
        📄 {{ node.name }}
      </a>
    </template>
  </li>
</template>


<script setup lang="ts">
import TreeNode from "./TreeNode.vue";
import {authFetch} from "@/requests";
import {defineProps, defineEmits, ref} from "vue";
//import { useUiStore } from '@/composables/store.js'
//const store = useUiStore()

const error = ref<string | null>(null)

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

async function updateFolder(fileId) {
  //store.setLoading(true)
  error.value = null;
  try {
    const response = await authFetch("transcript/folder/update/" + fileId);
    if (!response.ok) throw new Error("Network response was not ok");

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to update transcript.";
  } finally {
    //store.setLoading(false)
  }
}

const emit = defineEmits<{
  (e: "folder-clicked", fileId: string | number): void;
  (e: "transcript-clicked", fileId: string | number): void;
}>();

const clickedNodeFolder = (node: Node) => {
  emit("folder-clicked", node);
};

const clickedTranscript = (fileId: string | number) => {
  emit("transcript-clicked", fileId);
};
</script>