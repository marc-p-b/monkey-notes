<template xmlns="http://www.w3.org/1999/html">
<h2>prefs</h2>

  <div v-if="googleConnectRequired">
    <p>Google drive is disconnect. Please proceed to authentication using the following link</p>
    <a :href="googleAuthUrl">Google Drive auth</a>
  </div>

  <form @submit.prevent="submitForm">
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

  <button @click.prevent="googleDisconnect">Disconnect from Google Drive</button>
  <button @click.prevent="logout">Logout from Monkey Notes</button>

  <p>{{message}}</p>

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

const prefs = ref<Prefs[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const googleConnectRequired = ref(false)
const googleAuthUrl = ref<string | null>(null)

const message = ref(<string>"")

  const submitForm = async () => {

    console.log("save !")

    try {
      const response = await authFetch("http://localhost:8080/preferences/form", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(prefs.value),
      });

      if (!response.ok) {
        throw new Error(`Server error: ${response.status}`);
      }

      const data = await response.json();
      console.log("Response:", data);

      //success.value = true;
    } catch (err: any) {

      console.log("err " + err)

      //error.value = err.message || "Something went wrong.";
    } finally {
      console.log("fin")
      //loading.value = false;
    }
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

async function googleDisconnect() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("http://localhost:8080/drive/disconnect");
    if (!response.ok) throw new Error("Network response was not ok");
    message.value="Disconnected from Google Drive Account"
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load preferences.";
  } finally {
    loading.value = false;
  }
}

function logout() {
  window.location.href = "/logout";
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
    error.value = "Failed to load preferences.";
  } finally {
    loading.value = false;
  }
}

async function fetchGoogleAuth() {
  loading.value = true;
  error.value = null;

  try {
    const response = await authFetch("http://localhost:8080/preferences/authGoogleDrive");
    if (!response.ok) throw new Error("Network response was not ok");
    const g = await response.json();
    //console.log(g)

    googleConnectRequired.value = !g.connected
    googleAuthUrl.value = g.url


  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load google auth status.";
  } finally {
    loading.value = false;
  }
}

// Load on mount
onMounted(() => {
  fetchGoogleAuth();
  fetchPreferences();
});


</script>