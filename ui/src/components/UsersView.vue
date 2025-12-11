<template>
  <div class="home-wrapper">

    <h2>Users</h2>
    <ul>
      <li v-for="user in users">
        <span>{{user.username}} {{user.roles}}</span>
      </li>
    </ul>

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
  roles: []
}

const users = ref<DtoUser[]>([])

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
