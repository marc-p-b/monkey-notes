<template>

  <h2>Named Entities</h2>

  <ul>
    <li v-for="ne in neList">
      {{ne.value}}
    </li>
  </ul>

</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import {authFetch} from "@/requests";

interface DtoNEIdx {
  verb: string
  value: string
  username: string
  count: number
}

const neList = ref<DtoNEIdx[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function fetch() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("ne/verb/tag");
    if (!response.ok) throw new Error("Network response was not ok");
    neList.value = await response.json();
    //console.log(response.json())
    console.log(neList.value)
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load processes.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetch();

});

</script>
