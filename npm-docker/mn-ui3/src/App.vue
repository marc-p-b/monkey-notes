
<template>

  <header>


    <div class="wrapper">
      <HelloWorld msg="You did it!" />

      <nav>
        <RouterLink to="/">Home</RouterLink>
        <RouterLink to="/todo">Todo</RouterLink>
      </nav>
    </div>
  </header>


  <RouterView/>
</template>

<script setup>


const currentView = () => {

  const token = localStorage.getItem("token");
  console.log("token " + token);
  if (!token) {
    console.log("no token, login");
    return LoginForm;
  }
  return routes[currentPath.value.slice(1) || '/'] || NotFound;
}

console.log(currentView)

</script>

<style>
</style>