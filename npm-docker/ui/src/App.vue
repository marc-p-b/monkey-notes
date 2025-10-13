
<template>
  <Menubar :model="menuItems" />
  <RouterView/>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useRouter } from 'vue-router'
const router = useRouter()

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
    label: 'Processes',
    icon: 'pi pi-briefcase',
    command: () => router.push('/processes')
  }
])

</script>

<style>
</style>