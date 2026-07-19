<template>
  <div class="agent-shell">

    <div class="agent-header">
      <Button icon="pi pi-arrow-left" text size="small" @click="goBack" />
      <span class="agent-header-title">
        <i class="pi pi-bolt" /> AI Agent<span v-if="isMultiple"> &middot; {{ fileIds.length }} documents</span>
      </span>
      <Button :icon="settingsVisible ? 'pi pi-times' : 'pi pi-cog'" text size="small" @click="settingsVisible = !settingsVisible" />
    </div>

    <div v-if="settingsVisible" class="agent-settings">
      <div class="settings-row">
        <label>Model</label>
        <Select v-model="agentPrepare.model" :options="agentPrepare.availableAIModels" optionLabel="label" optionValue="name" class="settings-select" />
      </div>
      <div class="settings-row">
        <label>Instructions</label>
        <Textarea v-model="agentPrepare.instructions" autoResize rows="2" class="settings-textarea" />
      </div>
      <div class="settings-row">
        <label>Reset thread</label>
        <ToggleButton v-model="agentPrepare.reset" onLabel="Yes" offLabel="No" onIcon="pi pi-refresh" offIcon="pi pi-minus" size="small" />
      </div>
    </div>

    <div class="agent-messages" ref="messagesEl">
      <div v-if="!agentPrepare.messages || agentPrepare.messages.length === 0" class="agent-empty">
        <i class="pi pi-comments" style="font-size: 2rem; opacity: 0.3" />
        <p>Ask anything about this document.</p>
      </div>

      <div
        v-for="(msg, i) in agentPrepare.messages"
        :key="i"
        :class="['bubble-wrap', msg.messageDir === 'user' ? 'bubble-user' : 'bubble-assistant']"
      >
        <div class="bubble">{{ msg.content }}</div>
        <span class="bubble-time">{{ formatTime(msg.createdAt) }}</span>
      </div>

      <div v-if="requested" class="bubble-wrap bubble-assistant">
        <div class="bubble thinking">
          <span /><span /><span />
        </div>
      </div>
    </div>

    <div class="agent-input-bar">
      <Textarea
        v-model="agentPrepare.question"
        autoResize
        rows="1"
        placeholder="Ask something about this document…"
        class="agent-input"
        @keydown.enter.exact.prevent="submitForm"
      />
      <Button icon="pi pi-send" @click="submitForm" :disabled="requested || !agentPrepare.question?.trim()" />
    </div>

  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, nextTick } from "vue"
import { useRouter, useRoute } from 'vue-router'
import { authFetch } from "@/requests.ts"

interface DtoAgentMessage {
  messageDir: string
  content: string
  createdAt: string
}

interface AIModel{
  name: string
  label: string
}

interface DtoAgentPrepare {
  uuid: string
  model: string
  instructions: string
  availableAIModels: AIModel[]
  selectedAIModel?: string
  createdAt: string
  exists: boolean
  threadId?: string
  fileIds: string[]
  messages: DtoAgentMessage[]
  question: string
  reset: boolean
}

const props = defineProps<{ fileId?: string }>()
const router = useRouter()
const route = useRoute()

// ids can arrive as a single route param (single-document case, from TranscriptView)
// or as a comma-separated ?ids= query (multi-select case, from Home's select mode).
// The full set is sent to agent/prepare; primaryFileId is only used for single-doc
// UI concerns (e.g. the back button target).
const fileIds = computed<string[]>(() => {
  const idsParam = route.query.ids
  if (typeof idsParam === 'string' && idsParam.length) {
    return idsParam.split(',').map(id => id.trim()).filter(Boolean)
  }
  return props.fileId ? [props.fileId] : []
})
const primaryFileId = computed(() => fileIds.value[0])
const isMultiple = computed(() => fileIds.value.length > 1)

// a ?uuid= query resumes a past conversation from AgentListView, bypassing
// the fileIds-based prepare flow entirely.
const resumeUuid = computed(() => typeof route.query.uuid === 'string' ? route.query.uuid : undefined)

const goBack = () => {
  if (resumeUuid.value) {
    router.push('/agents')
  } else if (isMultiple.value || !primaryFileId.value) {
    router.push('/')
  } else {
    router.push('/transcript/' + primaryFileId.value)
  }
}

const agentPrepare = ref<DtoAgentPrepare>({
  model: '',
  instructions: '',
  availableAIModels: [],
  createdAt: '',
  exists: false,
  messages: [],
  question: '',
  reset: false,
})

const loading = ref(true)
const error = ref<string | null>(null)
const agentMessage = ref<string>()
const requested = ref(false)
const settingsVisible = ref(false)
const messagesEl = ref<HTMLElement | null>(null)
let eventSource: EventSource | null = null

const formatTime = (iso: string) => {
  if (!iso) return ''
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

async function prepareAgent() {
  if (!resumeUuid.value && fileIds.value.length === 0) {
    error.value = "No document selected."
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    const response = resumeUuid.value
      ? await authFetch("agent/prepare/" + resumeUuid.value)
      : await authFetch("agent/prepare?fileIds=" + encodeURIComponent(fileIds.value.join(',')))
    if (!response.ok) throw new Error("Network response was not ok")
    agentPrepare.value = await response.json()
    if (!agentPrepare.value.messages) agentPrepare.value.messages = []
    if (!agentPrepare.value.model) agentPrepare.value.model = agentPrepare.value.selectedAIModel ?? ''
    settingsVisible.value = !agentPrepare.value.exists
    await scrollToBottom()
  } catch (err: any) {
    console.error(err)
    error.value = "Failed to load agent."
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  const question = agentPrepare.value.question?.trim()
  if (!question || requested.value) return

  agentPrepare.value.messages.push({
    messageDir: 'user',
    content: question,
    createdAt: new Date().toISOString(),
  })
  agentPrepare.value.question = ''
  requested.value = true
  agentMessage.value = "Agent requested"
  await scrollToBottom()

  try {
    const response = await authFetch("agent/ask", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(agentPrepare.value),
    })
    if (!response.ok) throw new Error(`Server error: ${response.status}`)

    const data = await response.json()
    const streamUrl = window._env_.API_URL + "/" + data.url
    initStream(streamUrl)
  } catch (err: any) {
    error.value = err.message || "Something went wrong."
    requested.value = false
  }
}

const initStream = (url: string) => {
  if (eventSource) eventSource.close()

  const token = localStorage.getItem("token")
  eventSource = new EventSource(url + '/' + token)

  eventSource.onmessage = async (event) => {
    if (event.data !== 'waiting') {
      agentPrepare.value.messages.push({
        messageDir: 'assistant',
        content: event.data,
        createdAt: new Date().toISOString(),
      })
      requested.value = false
      eventSource?.close()
      await scrollToBottom()
    }
  }

  eventSource.onerror = (err) => {
    console.error('SSE error:', err)
    requested.value = false
    eventSource?.close()
  }
}

watch([fileIds, resumeUuid], () => {
  prepareAgent()
}, { immediate: true })
</script>

<style scoped>
.agent-shell {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  max-width: 860px;
  margin: 0 auto;
}

.agent-header {
  display: flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--p-surface-200);
  gap: 0.5rem;
}

.agent-header-title {
  flex: 1;
  font-weight: 600;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.agent-settings {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--p-surface-200);
  background: var(--p-surface-50);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.settings-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.settings-row label {
  width: 100px;
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
  flex-shrink: 0;
}

.settings-select {
  width: 200px;
}

.settings-textarea {
  flex: 1;
  font-size: 0.85rem;
}

.agent-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.agent-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: var(--p-text-muted-color);
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  max-width: 72%;
}

.bubble-user {
  align-self: flex-end;
  align-items: flex-end;
}

.bubble-assistant {
  align-self: flex-start;
  align-items: flex-start;
}

.bubble {
  padding: 0.6rem 0.9rem;
  border-radius: 1.1rem;
  font-size: 0.92rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-user .bubble {
  background: var(--p-primary-color);
  color: var(--p-primary-contrast-color, #fff);
  border-bottom-right-radius: 0.25rem;
}

.bubble-assistant .bubble {
  background: var(--p-surface-100);
  color: var(--p-text-color);
  border-bottom-left-radius: 0.25rem;
}

.bubble-time {
  font-size: 0.72rem;
  color: var(--p-text-muted-color);
  margin-top: 0.2rem;
  padding: 0 0.25rem;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0.7rem 1rem;
}

.thinking span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--p-text-muted-color);
  animation: bounce 1.2s infinite;
}

.thinking span:nth-child(2) { animation-delay: 0.2s; }
.thinking span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40%            { transform: translateY(-6px); opacity: 1; }
}

.agent-input-bar {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--p-surface-200);
}

.agent-input {
  flex: 1;
  resize: none;
  max-height: 140px;
  overflow-y: auto;
}
</style>