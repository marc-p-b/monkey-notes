<template>
  <div class="home-wrapper">

    <h2>Users</h2>
    <form>
      <ul>
        <li v-for="user in users" :key="user.username">
          <label :for="user.username">{{ user.username }}</label>

          <ToggleButton
              :id="user.username"
              v-model="user.admin"
              onLabel="Admin user"
              offLabel="Regular user"
              onIcon="pi pi-star"
              offIcon="pi pi-user"
          />

          <Button label="New password" />
          <Password
              id="password"
              v-model="newPassword"
              toggleMask
              :feedback="true"
              required
          />
          <Button label="Ok" @click.prevent="changePassword(user.username)"/>

        </li>
      </ul>
      <Button label="Save" type="submit" class="mt-4" />
    </form>

    <h2>Create new user</h2>
    <form @submit.prevent="createUser" class="p-fluid">
      <div class="p-field">
        <label for="username">Username</label>
        <InputText
            id="username"
            v-model="formCreateUser.username"
            required
        />
      </div>
      <ToggleButton
          v-model="formCreateUser.admin"
          onLabel="Admin user"
          offLabel="Regular user"
          onIcon="pi pi-star"
          offIcon="pi pi-user"
      />


      <span>{{password}}</span>

      <Button label="Create User" type="submit" class="mt-4" />
    </form>

  </div>
</template>

<script lang="ts" setup>
import {ref, onMounted, defineProps} from "vue";
import {authFetch} from "@/requests";

import { useRouter } from 'vue-router'
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)

interface DtoUser {
  username: string
  admin: boolean
  roles: []
}

const users = ref<DtoUser[]>([])

const password = ref();

const newPassword = ref();

const formCreateUser = ref({
  username: '',
  admin: false
});

const createUser = async () => {
  try {
    const response = await authFetch("user/create", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(formCreateUser.value),
    });

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }
    password.value = await response.text();
  } catch (err: any) {
    error.value = err.message || "Something went wrong.";
  } finally {
    loading.value = false;

  }
}

const changePassword = async (user) => {
  console.log("change pass " + user + newPassword.value)
  try {
    const url = "user/" + user + "/password"
    const json = "{'password':'" + newPassword.value + "'}"
    const response = await authFetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: json,
    });

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }
    password.value = await response.text();
  } catch (err: any) {
    error.value = err.message || "Something went wrong.";
  } finally {
    loading.value = false;

  }
}

async function fetchUsers() {
  loading.value = true
  error.value = null;
  try {
    const response = await authFetch("user/list");
    if (!response.ok) throw new Error("Network response was not ok");
    users.value = await response.json();
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load users.";
  } finally {
    loading.value = false
  }
}


onMounted(() => {
  fetchUsers()
});

</script>
