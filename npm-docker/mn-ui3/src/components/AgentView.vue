<template>

  <div id="container">

    <p>{{agentPrepare.instructions}}</p>

    <ul>
      <li v-for="message in agentPrepare.messages">

        {{message.content}}


      </li>


    </ul>


  </div>

</template>

<script lang="ts" setup>
import {ref, onMounted, defineProps} from "vue";
import { authFetch } from "@/requests.ts";
import a from "ansis";

interface DtoAgentMessage {
  messageDir: string
  content: string
  createdAt: string
}

interface DtoAgentPrepare {
  model: string
  instructions: string
  createdAt: string
  exists: boolean
  messages: DtoAgentMessage[]
}

const props = defineProps<{ fileId: string }>()
const agentPrepare = <DtoAgentPrepare[]>ref([])
const loading = ref(true)
const error = ref<string | null>(null)

async function prepareAgent() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("agent/prepare/' + fileId" + props.fileId);
    if (!response.ok) throw new Error("Network response was not ok");
    agentPrepare.value = await response.json();

    //console.log(response.json())

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  prepareAgent();
});

</script>
