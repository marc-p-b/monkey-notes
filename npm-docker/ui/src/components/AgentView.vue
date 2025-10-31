<template>

  <div id="container">

  <!--    <div v-if="connected">-->
  <!--      <ul>-->
  <!--        <li v-for="(msg, index) in streamMsg" :key="index">-->
  <!--          {{ msg }}-->
  <!--        </li>-->
  <!--      </ul>-->

  <!--    </div>-->

    <div v-if="requested">
      <ProgressSpinner style="width: 30px; height: 30px" strokeWidth="4" fill="transparent" animationDuration="2s" aria-label="Custom ProgressSpinner" />
      <span>{{agentMessage}}</span>
    </div>

    <div>
      <ul>
        <li v-for="message in agentPrepare.messages">
          {{message.content}}
        </li>
      </ul>
    </div>

    <form id="formAgent" @submit.prevent="submitForm">
      <fieldset>
        <label for="textAreaQuestion">Question</label>
        <textarea name="question" id="textAreaQuestion" v-model="agentPrepare.question"></textarea>
      </fieldset>
      <fieldset>
        <label for="inputResetId">Reset</label>
        <input type="checkbox" name="reset" id="inputResetId" v-model="agentPrepare.reset"/>
      </fieldset>
      <fieldset>
        <label for="selectModelId">Model</label>
        <select name="selectModel" id="selectModelId" v-model="agentPrepare.model">
          <option value="default">default</option>
          <option value="gpt-4o">gpt 4o</option>
          <option value="gpt-4o-mini">gtp 4o mini</option>
        </select>
      </fieldset>
      <fieldset>
        <label for="textAreaInstructionsId">Instructions</label>
        <textarea name="instructions" id="textAreaInstructionsId" v-model="agentPrepare.instructions"></textarea>
      </fieldset>
      <fieldset role="group">
        <button>Send</button>
      </fieldset>
      <input type="hidden" name="threadId" id="inputThreadId" v-model="agentPrepare.threadId"/>
      <input type="hidden" name="fileId" id="inputFileId" v-model="agentPrepare.fileId"/>
    </form>
  </div>

</template>

<script lang="ts" setup>
import {ref, onMounted} from "vue";
import { authFetch } from "@/requests.ts";

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
  //completed by this form
  question: string
  reset: boolean
}

const props = defineProps<{ fileId: string }>()
const agentPrepare = <DtoAgentPrepare[]>ref([])
const loading = ref(true)
const error = ref<string | null>(null)

//const streamMsg = ref<string[]>([])
const agentMessage = ref<string>()
const connected = ref(false)
const requested = ref(false)
let eventSource: EventSource | null = null

async function prepareAgent() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("agent/prepare/" + props.fileId);
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

const submitForm = async () => {
  requested.value = true;
  agentMessage.value = "Agent requested"
  try {
    const response = await authFetch("agent/ask", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(agentPrepare.value),
    });

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }

    const data = await response.json()
    const streamUrl = import.meta.env.VITE_API_URL + "/" + data.url

    if (!streamUrl) throw new Error('No streaming URL returned')

    //limit to 1 stream
    initStream(streamUrl)

  } catch (err: any) {
    error.value = err.message || "Something went wrong.";
  } finally {
    loading.value = false;
  }
}

const initStream = (url: string) => {
  console.log("stream from " + url)

  if (eventSource) {
    eventSource.close()
  }

  const token = localStorage.getItem("token")
  eventSource = new EventSource(url + '/' + token)

  eventSource.onopen = () => {
    //streamMsg.value.push('🔗 Connected to authenticated stream.')
    connected.value = true
  }

  eventSource.onmessage = (event) => {
    const sseMsg = event.data
      console.log("sse msg " + sseMsg)
    if(event.data === 'waiting') {
      //streamMsg.value.push(sseMsg)

    } else {
      //streamMsg.value.push('done !')

      const t: DtoAgentMessage = {
        messageDir: 'assistant',
        content: sseMsg,
        createdAt: new Date().toISOString()
      }

      if (!agentPrepare.value.messages || agentPrepare.value.messages.length === 0) {
        agentPrepare.value.messages = []
      }

      agentPrepare.value.messages.push(t)
      eventSource.close()
    }
  }

  eventSource.onerror = (err) => {
    console.error('SSE error:', err)
    eventSource.close()
  }

}

onMounted(() => {
  prepareAgent();
});

</script>
