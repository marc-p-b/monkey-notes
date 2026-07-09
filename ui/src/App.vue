
<template>
  <Menubar v-if="store.isConnected" :model="menuItems">
    <template #end>
      <div class="header-end">
        <ProgressSpinner v-if="store.loading" style="width: 30px; height: 30px" strokeWidth="8" animationDuration="2s"/>
        <InputText v-model="query" @keydown.enter="search" placeholder="Search..." class="header-search"/>
        <div class="user-chip" @click="toggleUserMenu">
          <i class="pi pi-user"/>
          <span>{{ userData?.username }}</span>
          <i class="pi pi-angle-down"/>
        </div>
        <Menu ref="userMenu" :model="userMenuItems" popup/>
      </div>
    </template>
  </Menubar>
  <RouterView v-slot="{ Component }">
    <KeepAlive include="Home">
      <component :is="Component" />
    </KeepAlive>
  </RouterView>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue"
import {authFetch} from "@/requests";
import { useRouter } from 'vue-router'
import { useUiStore } from '@/composables/store.js'

const router = useRouter()
const store = useUiStore()

const menuItems = computed(() => [
  {
    label: 'Home',
    icon: 'pi pi-home',
    command: () => router.push('/')
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
    label: 'Admin',
    icon: 'pi pi-users',
    visible: userData.value?.admin === true,
    command: () => router.push('/users')
  }
])

interface UserData {
  username: string
  admin: boolean
}

const query = ref<string>()
const userData = ref<UserData>()
const userMenu = ref()

const userMenuItems = ref([
  {
    label: 'Preferences',
    icon: 'pi pi-cog',
    command: () => router.push('/preferences')
  }, {
    label: 'Disconnect',
    icon: 'pi pi-sign-out',
    command: () => {
      localStorage.removeItem('token')
      store.refreshAuth()
      router.push('/login')
    }
  }
])

const toggleUserMenu = (event: Event) => {
  userMenu.value.toggle(event)
}

const search = () => {
  store.setSearch(query.value)
  router.push({ name: 'search' })
}

async function fetchCurrentUser() {
  try {
    const response = await authFetch("user/whoami");
    if (!response.ok) throw new Error("Network response was not ok");

    userData.value = await response.json();

  } catch (err: any) {
    console.error(err);
  }
}

onMounted(() => {
  fetchCurrentUser();
});
</script>

<style>
.header-end {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.header-search {
  width: 180px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.75rem;
  border-radius: 2rem;
  background: var(--p-primary-color, #6366f1);
  color: var(--p-primary-contrast-color, #fff);
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  user-select: none;
  transition: opacity 0.15s;
}

.user-chip:hover {
  opacity: 0.85;
}
</style>