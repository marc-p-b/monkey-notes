<template xmlns="http://www.w3.org/1999/html">

  <div class="main-wrapper">
    <h2>Preferences</h2>

    <h3>Hello, {{prefs.username}}</h3>
    <form>
      <Fieldset legend="Accounts">
        <div class="inline-actions">
          <Button @click.prevent="logout" label="Logout from Monkey Notes"/>
          <Button icon="pi pi-key" label="Change Password" severity="secondary" @click.prevent="showPasswordDialog = true" />
        </div>
      </Fieldset>
    </form>

    <Dialog v-model:visible="showPasswordDialog" modal header="Change Password" :style="{ width: '420px' }">
      <div class="dialog-form">
        <div class="field">
          <label>New password</label>
          <Password v-model="newPassword" toggleMask :feedback="true" class="w-full" inputClass="w-full" />
        </div>
      </div>
      <div class="dialog-footer">
        <Button label="Cancel" text @click="showPasswordDialog = false" />
        <Button label="Save" icon="pi pi-check" :loading="changingPassword" @click="changePassword" />
      </div>
    </Dialog>

    <form>
      <Fieldset legend="Data Management">
        <div class="inline-actions">
          <Button label="Export" icon="pi pi-download" @click="downloadExport" />
          <FileUpload mode="basic" chooseLabel="Import" chooseIcon="pi pi-upload" @select="handleFileSelect($event)" customUpload auto severity="secondary" />
          <Button label="Wipe all data" icon="pi pi-trash" severity="danger" @click="wipe" />
        </div>
      </Fieldset>
    </form>

    <form @submit.prevent="submitForm">

      <Fieldset legend="Sync Option">
        <SelectButton
            v-model="prefs.syncOption"
            :options="syncOptions"
            optionLabel="label"
            optionValue="value"
        />
      </Fieldset>


      <Fieldset v-if="prefs.syncOption === 'gdrive'" legend="Google Drive Input Folder ID">
        <InputText
            v-model="prefs.inputFolderId"
            placeholder="google drive id"
            class="w-full"
        />
        <div v-if="googleConnectRequired">
          <p>Google drive is disconnect. Please proceed to authentication using the following link</p>
          <a :href="googleAuthUrl">Google Drive auth</a>
        </div>
        <div v-else>
          <Button @click.prevent="googleDisconnect" label="Disconnect from Google Drive"/>
        </div>
        <Button @click.prevent="updateAllTranscripts" label="Update all folders and transcripts"/>
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

    <ConfirmDialog />

    <Dialog v-model:visible="errorDialogVisibility" modal header="Error !" :style="{ width: '50rem' }" :breakpoints="{ '1199px': '75vw', '575px': '90vw' }">
      <p class="mb-8">
        {{message}}
      </p>
    </Dialog>

  </div>

</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { authFetch } from "@/requests.ts";
import {authPostFile} from "../requests";

import { useConfirm } from "primevue/useconfirm";

const confirm = useConfirm();

const syncOptions = [
  { label: "No Sync", value: "none" },
  { label: "Google Drive Sync", value: "gdrive" },
  { label: "Monkey Notes Companion App Sync", value: "monkey" }
];



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

  syncOption: string

}

const prefs = ref<Prefs[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const googleConnectRequired = ref(false)
const googleAuthUrl = ref<string | null>(null)

const message = ref(<string>"")
const errorDialogVisibility = ref(false)

const showPasswordDialog = ref(false)
const newPassword = ref('')
const changingPassword = ref(false)


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

  // console.log(prefs.value.syncOption)
  // console.log(prefs.value.inputFolderId)

  if(prefs.value.syncOption === 'gdrive' && prefs.value.inputFolderId === '') {
    message.value = 'Google drive input folder id is required.'
    errorDialogVisibility.value = true
    // console.log('error')
    //errorDialogVisibility=true
    return
  }

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

    console.log(prefs.value.syncOption);

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

function wipe() {
  confirm.require({
    message: 'Are you sure you want to wipe all data? This cannot be undone.',
    header: 'Confirm',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      await authFetch("data/wipe", { method: "DELETE" });
      message.value = "All data wiped";
    },
    reject: () => {
      // optional: do nothing or show cancelled message
    }
  });
}


async function changePassword() {
  changingPassword.value = true
  try {
    const response = await authFetch("user/me/password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password: newPassword.value }),
    })
    if (!response.ok) throw new Error(`Server error: ${response.status}`)
    showPasswordDialog.value = false
    newPassword.value = ''
  } catch (err: any) {
    message.value = err.message || 'Failed to change password.'
    errorDialogVisibility.value = true
  } finally {
    changingPassword.value = false
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

.inline-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-top: 0.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--p-text-muted-color, #6b7280);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

</style>