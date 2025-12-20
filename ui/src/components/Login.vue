<template>

  <div class="login-container">
    <Card class="login-card">
      <template #title>
        Login
      </template>

      <template #content>
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="field">
            <label for="username">Username</label>
            <InputText
                id="username"
                v-model="username"
                autocomplete="username"
                :disabled="loading"
            />
          </div>

          <div class="field">
            <label for="password">Password</label>
            <Password
                id="password"
                v-model="password"
                toggleMask
                autocomplete="current-password"
                :feedback="false"
                :disabled="loading"
            />
          </div>

          <Button
              label="Login"
              type="submit"
              class="w-full"
              :loading="loading"
          />

          <Message v-if="errorMessage" severity="error" class="mt-3">
            {{ errorMessage }}
          </Message>

          <Message v-if="infoMessage" severity="info" class="mt-3">
            {{ infoMessage }}
          </Message>
        </form>
      </template>
    </Card>
  </div>

</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-ground);
}

.login-card {
  width: 100%;
  max-width: 400px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
</style>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { noAuthFetch } from "@/requests.ts";
import { onBeforeUnmount } from 'vue'

const router = useRouter();

const username = ref("");
const password = ref("");
const errorMessage = ref("");
const infoMessage = ref("");

async function handleLogin() {
  try {
    const response = await noAuthFetch('jwt/login', {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: username.value,
        password: password.value
      })
    });

    if (!response.ok) {
      errorMessage.value = "Invalid credentials";
      return;
    } else {
      infoMessage.value = "Login successfull, redirect is 2 secs"
      redirectAfterLogin();
    }

    const data = await response.json();
    const token = data.token; // adjust to match backend
    localStorage.setItem("token", token);

    // if (localStorage.getItem("requestedPath")) {
    //   //await delay(2000); //TODO add a delay here
    //   const afterLoginPath = localStorage.getItem("requestedPath")
    //   localStorage.removeItem("requestedPath")
    //   router.push(afterLoginPath)
    // }


  } catch (err) {
    errorMessage.value = "Server error";
    console.error(err);
  }
}



let redirectTimer

const redirectAfterLogin = () => {
  redirectTimer = setTimeout(() => {
    router.push({ name: 'home' })
  }, 2000)
}

onBeforeUnmount(() => {
  if (redirectTimer) clearTimeout(redirectTimer)
})

</script>