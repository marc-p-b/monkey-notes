<template>
  <div class="main-wrapper">

    <div class="page-header">
      <h2>Users</h2>
      <Button label="Add User" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <Message v-if="feedback.text" :severity="feedback.severity" :closable="true" @close="feedback.text = ''" class="feedback-msg">
      {{ feedback.text }}
    </Message>

    <DataTable :value="users" class="users-table">
      <Column field="username" header="Username" style="width: 20%" />
      <Column field="email" header="Email" style="width: 35%">
        <template #body="{ data }">
          <InputText v-model="data.email" class="w-full" />
        </template>
      </Column>
      <Column field="admin" header="Role" style="width: 20%">
        <template #body="{ data }">
          <ToggleButton
            v-model="data.admin"
            onLabel="Admin"
            offLabel="User"
            onIcon="pi pi-star"
            offIcon="pi pi-user"
          />
        </template>
      </Column>
      <Column header="Actions" style="width: 25%">
        <template #body="{ data }">
          <Button
            icon="pi pi-key"
            label="Password"
            text
            size="small"
            @click="openPasswordDialog(data.username)"
          />
        </template>
      </Column>
    </DataTable>

    <div class="save-row">
      <Button label="Save changes" icon="pi pi-check" :loading="saving" @click="saveList" />
    </div>

    <!-- Create user dialog -->
    <Dialog v-model:visible="showCreateDialog" modal header="Create User" :style="{ width: '420px' }">
      <form @submit.prevent="createUser" class="dialog-form">
        <div class="field">
          <label for="new-username">Username</label>
          <InputText id="new-username" v-model="formCreateUser.username" required class="w-full" />
        </div>
        <div class="field">
          <label for="new-email">Email</label>
          <InputText id="new-email" v-model="formCreateUser.email" type="email" required class="w-full" />
        </div>
        <div class="field">
          <ToggleButton
            v-model="formCreateUser.admin"
            onLabel="Admin user"
            offLabel="Regular user"
            onIcon="pi pi-star"
            offIcon="pi pi-user"
          />
        </div>
        <div v-if="generatedPassword" class="generated-password">
          <span class="generated-password-label">Generated password:</span>
          <code>{{ generatedPassword }}</code>
        </div>
        <div class="dialog-footer">
          <Button label="Cancel" text @click="showCreateDialog = false" />
          <Button label="Create" type="submit" icon="pi pi-check" :loading="creating" />
        </div>
      </form>
    </Dialog>

    <!-- Change password dialog -->
    <Dialog v-model:visible="showPasswordDialog" modal :header="`Change password — ${passwordTarget}`" :style="{ width: '420px' }">
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

  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue"
import { authFetch } from "@/requests"

interface DtoUser {
  username: string
  email: string
  admin: boolean
}

const users = ref<DtoUser[]>([])
const saving = ref(false)
const creating = ref(false)
const changingPassword = ref(false)

const feedback = ref({ text: '', severity: 'success' as 'success' | 'error' })

const showCreateDialog = ref(false)
const generatedPassword = ref('')
const formCreateUser = ref({ username: '', email: '', admin: false })

const showPasswordDialog = ref(false)
const passwordTarget = ref('')
const newPassword = ref('')

function openCreateDialog() {
  formCreateUser.value = { username: '', email: '', admin: false }
  generatedPassword.value = ''
  showCreateDialog.value = true
}

function openPasswordDialog(username: string) {
  passwordTarget.value = username
  newPassword.value = ''
  showPasswordDialog.value = true
}

function showFeedback(text: string, severity: 'success' | 'error' = 'success') {
  feedback.value = { text, severity }
}

const saveList = async () => {
  saving.value = true
  try {
    const response = await authFetch("user/list/save", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(users.value),
    })
    if (!response.ok) throw new Error(`Server error: ${response.status}`)
    showFeedback('Users saved.')
  } catch (err: any) {
    showFeedback(err.message || 'Failed to save.', 'error')
  } finally {
    saving.value = false
  }
}

const createUser = async () => {
  creating.value = true
  generatedPassword.value = ''
  try {
    const response = await authFetch("user/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(formCreateUser.value),
    })
    if (!response.ok) throw new Error(`Server error: ${response.status}`)
    generatedPassword.value = await response.text()
    await fetchUsers()
  } catch (err: any) {
    showFeedback(err.message || 'Failed to create user.', 'error')
    showCreateDialog.value = false
  } finally {
    creating.value = false
  }
}

const changePassword = async () => {
  changingPassword.value = true
  try {
    const response = await authFetch(`user/${passwordTarget.value}/password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password: newPassword.value }),
    })
    if (!response.ok) throw new Error(`Server error: ${response.status}`)
    showFeedback(`Password updated for ${passwordTarget.value}.`)
    showPasswordDialog.value = false
  } catch (err: any) {
    showFeedback(err.message || 'Failed to change password.', 'error')
  } finally {
    changingPassword.value = false
  }
}

async function fetchUsers() {
  try {
    const response = await authFetch("user/list")
    if (!response.ok) throw new Error("Network response was not ok")
    users.value = await response.json()
  } catch (err: any) {
    showFeedback('Failed to load users.', 'error')
  }
}

onMounted(fetchUsers)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.page-header h2 {
  margin: 0;
}

.feedback-msg {
  margin-bottom: 1rem;
}

.users-table {
  margin-bottom: 1rem;
}

.save-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.75rem;
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

.generated-password {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.75rem;
  background: var(--p-surface-100, #f3f4f6);
  border-radius: 6px;
  font-size: 0.875rem;
}

.generated-password-label {
  color: var(--p-text-muted-color, #6b7280);
  white-space: nowrap;
}

.generated-password code {
  font-family: monospace;
  font-weight: 600;
  word-break: break-all;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.w-full {
  width: 100%;
}
</style>
