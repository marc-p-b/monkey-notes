<script setup lang="ts">
import HelloWorld from './components/HelloWorld.vue'
import Recents from "./components/Recents.vue";
import LoginForm from "./components/LoginForm.vue";

import { ref, computed } from 'vue'

const routes = {
  '/': Recents,
  '/login': LoginForm
}

const currentPath = ref(window.location.hash)

window.addEventListener('hashchange', () => {
  currentPath.value = window.location.hash
})

const currentView = computed(() => {
  return routes[currentPath.value.slice(1) || '/'] || Recents //Recents here is not found page !
})

</script>

<template>
<!--  <LoginForm/>-->

  <a href="#/">Recents</a> |
  <a href="#/login">Login</a> |
<!--  <a href="#/non-existent-path">Broken Link</a>-->
  <component :is="currentView" />

</template>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
