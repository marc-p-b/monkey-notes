
<template>
  <Menubar :model="menuItems">
    <template #end>
      <ProgressSpinner v-if="store.loading" style="width: 30px; height: 30px" strokeWidth="8" animationDuration="2s"/>
      <span>Hello, {{username}}</span>
      <InputText v-model="query" @keydown.enter="search"></InputText>
    </template>
  </Menubar>
  <RouterView/>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue"
import {authFetch} from "@/requests";
import { useRouter } from 'vue-router'
const router = useRouter()
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

const menuItems = ref([
  {
    label: 'Home',
    icon: 'pi pi-home',
    command: () => router.push('/')
  },
  {
    label: 'Preferences',
    icon: 'pi pi-cog',
    command: () => router.push('/preferences')
  },
  {
    label: 'Named Entities',
    icon: 'pi pi-tag',
    command: () => router.push('/ne')
  },
  {
    label: 'Processes',
    icon: 'pi pi-briefcase',
    command: () => router.push('/processes')
  },
  {
    label: 'Users',
    icon: 'pi pi-users',
    command: () => router.push('/users')
  },

])

const query = ref<string>()
const username = ref<string>()

const search = () => {
  store.setSearch(query)
  router.push({
    name: 'search'
  })
}

async function fetchCurrentUser() {
  try {
    const response = await authFetch("user/whoami");
    if (!response.ok) throw new Error("Network response was not ok");
    username.value = await response.text();
  } catch (err: any) {
    console.error(err);
  } finally {
  }
}

onMounted(() => {
  fetchCurrentUser();
});

</script>

<style>
</style>