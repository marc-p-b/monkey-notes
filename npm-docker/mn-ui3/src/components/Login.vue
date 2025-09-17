



<template>

  <div class="login">
    <h2>Login</h2>
    <form @submit.prevent="handleLogin">
      <input v-model="username" placeholder="Username" />
      <input v-model="password" type="password" placeholder="Password" />
      <button type="submit">login</button>
    </form>

    <p v-if="error">{{ error }}</p>
    <p v-if="infoMessage">{{ infoMessage }}</p>

  </div>
</template>

<script setup>
import { ref } from "vue";

const username = ref("");
const password = ref("");
const errorMessage = ref("");
const infoMessage = ref("");

async function handleLogin() {
  try {
    const response = await fetch("http://localhost:8080/jwt/login", {
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
      infoMessage.value = "ok !"
    }

    const data = await response.json();
    const token = data.token; // adjust to match backend

    localStorage.setItem("token", token);

    // Redirect to dashboard
    //router.push("/dashboard");
  } catch (err) {
    errorMessage.value = "Server error";
    console.error(err);
  }
}
</script>