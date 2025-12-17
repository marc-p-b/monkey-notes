<template>
  <div class="home-wrapper">
    <h2>Processes</h2>

    <p v-if="processes.length === 0">
      No process currently running
    </p>

    <div v-else>
      <ul>
        <li v-for="process in processes">
          <p v-if="process.status !== 'running'">{{process.name}} {{process.description}} (completed, ran for {{process.duration}})</p>
          <p v-else> {{process.name}} @{{process.username}} ({{process.description}}) running for {{process.duration}} <a @click.prevent="cancelProcess(process.id)">cancel</a></p>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import {authFetch} from "@/requests";

interface DtoProcess {
  id: string
  name: string
  statusStr: string
  status: string
  description: string
  duration: string
  username: string
}

const processes = ref<DtoProcess[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function fetchProcesses() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("process/list");
    if (!response.ok) throw new Error("Network response was not ok");
    processes.value = await response.json();

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load processes.";
  } finally {
    loading.value = false;
  }
}

async function cancelProcess(id) {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("process/cancel/" + id);
    if (!response.ok) throw new Error("Network response was not ok");

    //console.log(response)

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to cancel process";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchProcesses();
});

</script>
