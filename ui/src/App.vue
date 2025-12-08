
<template>
  <Menubar :model="menuItems">
    <template #end>
      <ProgressSpinner v-if="store.loading" style="width: 30px; height: 30px" strokeWidth="8" animationDuration="2s"/>
      <InputText v-model="query" @keydown.enter="search"></InputText>
    </template>
  </Menubar>
  <RouterView/>
</template>

<script setup lang="ts">
import { ref } from "vue"
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
  }

])

const query = ref<string>()

const search = () => {
  store.setSearch(query)
  router.push({
    name: 'search'
  })
}

</script>

<style>
</style>