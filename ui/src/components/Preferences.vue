<template xmlns="http://www.w3.org/1999/html">

  <div class="main-wrapper">
    <h2>Preferences</h2>

    <h3>Hello, {{prefs.username}}</h3>
    <form>
      <Fieldset legend="Accounts">
        <div class="actions">
          <Button @click.prevent="logout" label="Logout from Monkey Notes"/>
          <div v-if="googleConnectRequired">
            <p>Google drive is disconnect. Please proceed to authentication using the following link</p>
            <a :href="googleAuthUrl">Google Drive auth</a>
          </div>
          <div v-else>
            <Button @click.prevent="googleDisconnect" label="Disconnect from Google Drive"/>
          </div>
          <Button @click.prevent="updateAllTranscripts" label="Update all folders and transcripts"/>
        </div>

      </Fieldset>
    </form>

    <form>
      <Fieldset legend="Export data" class="data-fieldset">
        <div class="actions">
          <Button label="Download all data" @click="downloadExport" />
        </div>
      </Fieldset>

      <Fieldset legend="Import data" class="data-fieldset">
        <div class="actions">
          <FileUpload mode="basic" @select="handleFileSelect($event)" customUpload auto severity="secondary"  />
        </div>
      </Fieldset>
    </form>

    <form @submit.prevent="submitForm">
      <Fieldset legend="Google Drive Input Folder ID">
        <InputText
            v-model="prefs.inputFolderId"
            placeholder="google drive id"
            class="w-full"
        />
      </Fieldset>

      <Fieldset legend="Crop image to content">
        <ToggleButton
            v-model="prefs.cropImage"
            onLabel="Yes"
            offLabel="No"
            onIcon="pi pi-check"
            offIcon="pi pi-times"
        />
      </Fieldset>

      <Fieldset legend="OCR model">
        <Select v-model="prefs.selectedOcrModel" :options="prefs.ocrModels" placeholder="Select a model" class="w-full" />
      </Fieldset>

      <Fieldset legend="OCR Prompt">
        <InputText v-model="prefs.ocrPrompt" class="w-full" :placeholder="prefs.defaultOcrPrompt"/>
      </Fieldset>

      <Fieldset legend="OCR Read Timeout">
        <InputText
            v-model="prefs.qwenReadTimeout"
            :placeholder="prefs.dftQwenReadTimeout"
            class="w-full"
        />
      </Fieldset>

      <Fieldset legend="OCR Connect Timeout">
        <InputText
            v-model="prefs.qwenConnectTimeout"
            :placeholder="prefs.dftQwenConnectTimeout"
            class="w-full"
        />
      </Fieldset>

      <Fieldset legend="OCR Max Tokens">
        <InputText
            v-model="prefs.ocrMaxTokens"
            :placeholder="prefs.dftQwenMaxTokens"
            class="w-full"
        />
      </Fieldset>

      <Fieldset legend="Agent Instruction">
        <InputText v-model="prefs.agentInstructions" :placeholder="prefs.dftAgentInstructions" class="w-full" />
      </Fieldset>

      <Fieldset>
        <Button label="Save" type="submit" class="mr-2" />
        <Button label="Reset" type="button" @click="reset" class="p-button-secondary" />
      </Fieldset>
    </form>
  </div>

  <p>{{message}}</p>

</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { authFetch } from "@/requests.ts";
import {authPostFile} from "../requests";

export interface Prefs {
  set: boolean
  ocrPrompt?: string
  agentInstructions?: string
  model?: string
  inputFolderId?: string
  qwenConnectTimeout: number
  qwenReadTimeout: number
  ocrMaxTokens: number
  username: string
  ocrModels: string[]
  selectedOcrModel: string
  cropImage: boolean

  defaultOcrPrompt: string
  dftQwenMaxTokens: number
  dftQwenConnectTimeout: number
  dftQwenReadTimeout: number
  dftAgentInstructions: string


  /*
      private int dftQwenMaxTokens;
    private int dftQwenConnectTimeout;
    private int dftQwenReadTimeout;
    private String dftAgentInstructions;
   */

}

const prefs = ref<Prefs[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const googleConnectRequired = ref(false)
const googleAuthUrl = ref<string | null>(null)

const message = ref(<string>"")

const handleFileSelect = (event) => {
  const file = event.files[0]
  const formData = new FormData();
  formData.append("file", file);
  authPostFile("data/import", formData);
 }

async function downloadExport() {
  const res = await authFetch("data/export")
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = "monkey-notes.zip"
  link.click()

  URL.revokeObjectURL(url)
}


const submitForm = async () => {
  try {
    const response = await authFetch("preferences/form", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(prefs.value),
    });

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }
    } catch (err: any) {
      error.value = err.message || "Something went wrong.";
    } finally {
      loading.value = false;
    }
  }

const reset = () => {
  console.log("reset !")
}

async function googleDisconnect() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("drive/disconnect");
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
    const response = await authFetch("preferences/get");
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
    const response = await authFetch("preferences/authGoogleDrive");
    if (!response.ok) throw new Error("Network response was not ok");
    const data = await response.json();
    googleConnectRequired.value = !data.connected
    googleAuthUrl.value = data.url
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load google auth status.";
  } finally {
    loading.value = false;
  }
}

async function updateAllTranscripts() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("transcript/update/all");
    if (!response.ok) throw new Error("Network response was not ok");
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to update all transcripts.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchGoogleAuth();
  fetchPreferences();
});
</script>

<style>
.main-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}
.w-full {
  width: 100%;
}

.actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1rem;
}

</style>