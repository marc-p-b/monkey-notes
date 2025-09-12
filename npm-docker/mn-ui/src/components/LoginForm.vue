<template>
  <div class="login">
    <h2>Login</h2>
    <form @submit.prevent="login">
      <input v-model="username" placeholder="Username" />
      <input v-model="password" type="password" placeholder="Password" />
      <button type="submit">Login</button>
    </form>
    <p v-if="error">{{ error }}</p>
  </div>
</template>

<script>
export default {
  data() {
    return {
      username: "",
      password: "",
      error: ""
    };
  },
  methods: {
    async login() {
      try {
        const response = await fetch("http://localhost:8080/jwt/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username: this.username,
            password: this.password
          })
        });

        if (!response.ok) {
          throw new Error("Login failed");
        }

        const data = await response.json();

        // store JWT in localStorage
        localStorage.setItem("token", data.token);

        // redirect or update UI
        //this.$router.push("/dashboard");

        this.error = "LOGIN OK";

      } catch (err) {
        this.error = "Invalid username or password";
      }
    }
  }
};
</script>
