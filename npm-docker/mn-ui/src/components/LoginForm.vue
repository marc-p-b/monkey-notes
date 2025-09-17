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


<script lang="ts">
import { defineComponent, ref } from "vue";
//import { useRouter } from "vue-router";

export default defineComponent({
  setup() {
    const username = ref("");
    const password = ref("");
    const error = ref("");
    //const router = useRouter();

    const login = async () => {
      try {
        const res = await fetch("http://localhost:8080/jwt/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: username.value, password: password.value })
        });

        if (!res.ok) throw new Error("Login failed");

        const data = await res.json();
        localStorage.setItem("token", data.token);
        router.push("/");
      } catch (err) {
        error.value = "Invalid username or password";
      }
    };

    return { username, password, error, login };
  }
});
</script>

<!--<script>-->
<!--export default {-->
<!--  data() {-->
<!--    return {-->
<!--      username: "",-->
<!--      password: "",-->
<!--      error: ""-->
<!--    };-->
<!--  },-->
<!--  methods: {-->
<!--    async login() {-->
<!--      try {-->
<!--        const response = await fetch("http://localhost:8080/jwt/login", {-->
<!--          method: "POST",-->
<!--          headers: { "Content-Type": "application/json" },-->
<!--          body: JSON.stringify({-->
<!--            username: this.username,-->
<!--            password: this.password-->
<!--          })-->
<!--        });-->

<!--        if (!response.ok) {-->
<!--          throw new Error("Login failed");-->
<!--        }-->

<!--        const data = await response.json();-->

<!--        console.log("set token " + data.token);-->

<!--        // store JWT in localStorage-->
<!--        localStorage.setItem("token", data.token);-->

<!--        // redirect or update UI-->
<!--        this.$router.push("/");-->



<!--      } catch (err) {-->
<!--        this.error = "Invalid username or password";-->
<!--      }-->
<!--    }-->
<!--  }-->
<!--};-->
<!--</script>-->
