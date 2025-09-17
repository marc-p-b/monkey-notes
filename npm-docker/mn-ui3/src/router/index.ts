import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../components/Home.vue'
import LoginView from '../components/Login.vue'
import TodoView from '../components/Todo.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/todo',
      name: 'todo',
      component: TodoView,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    }
  ],
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");

  if (to.path !== "/login" && !token) {
    next("/login");
  } else {
    next();
  }
});

export default router
