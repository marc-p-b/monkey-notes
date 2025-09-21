<template xmlns="http://www.w3.org/1999/html">
<h2>prefs</h2>

  <form action="" @submit.prevent="save">
    <fieldset role="group">
      <label>Use Default prompt ?</label>
      <input type="checkbox" v-model="prefs.useDefaultPrompt" />
    </fieldset>

    <fieldset role="group">
      <label>Default prompt</label>
      <input type="text" v-model="prefs.prompt" />
    </fieldset>

    <fieldset role="group">
      <label>Default agent instruction</label>
      <input type="text" v-model="prefs.agentInstructions" />
    </fieldset>

    <fieldset role="group">
      <label>Default model</label>
      <input type="text" v-model="prefs.model" />
    </fieldset>

    <fieldset role="group">
      <label>Input folder id</label>
      <input type="text" v-model="prefs.inputFolderId" placeholder="google drive id"/>
    </fieldset>

    <fieldset role="group">
      <label>Output folder id</label>
      <input type="text" v-model="prefs.outputFolderId" placeholder="google drive id"/>
    </fieldset>

    <fieldset role="group">
      <label>Use default Ai connect timeout</label>
      <input type="checkbox" v-model="prefs.useDefaultAiConnectTimeout" />
    </fieldset>

    <fieldset role="group">
      <label>Ai read timeout</label>
      <input type="text" v-model="prefs.aiConnectTimeout" placeholder="timeout"/>
    </fieldset>

    <fieldset role="group">
      <label>Use default model max token</label>
      <input type="checkbox" v-model="prefs.useDefaultModelMaxTokens" />
    </fieldset>

    <fieldset role="group">
      <label>Model max token</label>
      <input type="text" v-model="prefs.modelMaxTokens" placeholder="timeout"/>
    </fieldset>

    <fieldset role="group">

      <button>Save</button>
      <button @click.prevent="reset">Reset</button>
    </fieldset>

  </form>

</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";

export interface Prefs {
  set: boolean;

  useDefaultPrompt: boolean;
  prompt?: string;

  agentInstructions?: string;

  useDefaultModel: boolean;
  model?: string;

  inputFolderId?: string;
  outputFolderId?: string;

  useDefaultAiConnectTimeout: boolean;
  aiConnectTimeout: number;

  useDefaultAiReadTimeout: boolean;
  aiReadTimeout: number;

  useDefaultModelMaxTokens: boolean;
  modelMaxTokens: number;
}

const prefs = ref<Prefs[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);


const save = () => {
  console.log("save !")

  


}

const reset = () => {
  console.log("reset !")
}

async function authFetch(url, options = {}) {
  const token = localStorage.getItem("token");

  const headers = {
    ...(options.headers || {}),
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };

  const response = await fetch(url, { ...options, headers });

  if (response.status === 401) {
    // optional: handle expired/invalid token
    localStorage.removeItem("token");
    window.location.href = "/login";
  }

  //console.log(response.json());

  return response;
}

async function fetchPreferences() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("http://localhost:8080/preferences/get");
    if (!response.ok) throw new Error("Network response was not ok");
    prefs.value = await response.json();
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load transcripts.";
  } finally {
    loading.value = false;
  }
}

// Load on mount
onMounted(() => {
  fetchPreferences();
});


</script>