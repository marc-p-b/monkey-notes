<template xmlns="http://www.w3.org/1999/html">

  <div class="main-wrapper">
    <h2>Preferences</h2>

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

      <!-- Use Default Prompt -->
      <Fieldset legend="Use Default Prompt">
        <ToggleButton
            v-model="prefs.useDefaultPrompt"
            onLabel="Yes"
            offLabel="No"
            onIcon="pi pi-check"
            offIcon="pi pi-times"
        />
      </Fieldset>

      <!-- Default Prompt -->
      <Fieldset legend="Default Prompt">
        <InputText v-model="prefs.prompt" class="w-full" />
      </Fieldset>

      <!-- Default Agent Instruction -->
      <Fieldset legend="Default Agent Instruction">
        <InputText v-model="prefs.agentInstructions" class="w-full" />
      </Fieldset>

      <!-- Default Model -->
      <Fieldset legend="Default Model">
        <InputText v-model="prefs.model" class="w-full" />
      </Fieldset>

      <!-- Input Folder ID -->
      <Fieldset legend="Input Folder ID">
        <InputText
            v-model="prefs.inputFolderId"
            placeholder="google drive id"
            class="w-full"
        />
      </Fieldset>

      <!-- Output Folder ID -->
      <Fieldset legend="Output Folder ID">
        <InputText
            v-model="prefs.outputFolderId"
            placeholder="google drive id"
            class="w-full"
        />
      </Fieldset>

      <!-- Use Default AI Connect Timeout -->
      <Fieldset legend="Use Default AI Connect Timeout">
        <ToggleButton
            v-model="prefs.useDefaultAiConnectTimeout"
            onLabel="Yes"
            offLabel="No"
            onIcon="pi pi-check"
            offIcon="pi pi-times"
        />
      </Fieldset>

      <!-- AI Read Timeout -->
      <Fieldset legend="AI Read Timeout">
        <InputText
            v-model="prefs.aiConnectTimeout"
            placeholder="timeout"
            class="w-full"
        />
      </Fieldset>

      <!-- Use Default Model Max Token -->
      <Fieldset legend="Use Default Model Max Token">
        <ToggleButton
            v-model="prefs.useDefaultModelMaxTokens"
            onLabel="Yes"
            offLabel="No"
            onIcon="pi pi-check"
            offIcon="pi pi-times"
        />
      </Fieldset>

      <!-- Model Max Token -->
      <Fieldset legend="Model Max Token">
        <InputText
            v-model="prefs.modelMaxTokens"
            placeholder="timeout"
            class="w-full"
        />
      </Fieldset>

      <!-- Buttons -->
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

 // async function handleFileSelect() {
 //   console.log("hey")
 // }


const handleFileSelect = (event) => {
  // event.files is always an array, even if single file mode
  const file = event.files[0]
  console.log('Selected file:', file)


// async function handleFileSelect(event) {
//   console.log("hey")
//   const file = event.target.files[0];
//
  const formData = new FormData();
  formData.append("file", file);

  authPostFile("data/import", formData);

  console.log("Upload done!");
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

    console.log(response)

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