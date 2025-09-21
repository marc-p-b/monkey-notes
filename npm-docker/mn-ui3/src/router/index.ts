import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../components/Home.vue'
import LoginView from '../components/Login.vue'
import PreferencesView from "../components/Preferences.vue";
import { jwtDecode } from "jwt-decode";

interface JwtPayload {
  sub: string;
  exp: number;
  iat?: number;
  roles?: string[];
}

export function isTokenValid(token: string | null): boolean {
  if (!token) return false;

  try {
    const decoded = jwtDecode<JwtPayload>(token);

    if (!decoded.exp) return false;

    const now = Date.now() / 1000; // seconds
    return decoded.exp > now;
  } catch (err) {
    return false;
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/preferences',
      name: 'prefs',
      component: PreferencesView,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    }
  ],
})

router.beforeEach((to, from, next) => {



  if (!isTokenValid(localStorage.getItem("token")) && to.path !== "/login") {
    localStorage.setItem("requestedPath", to.path)
    console.log("token is invalid, req is " + to.path)
    next("/login");
  } else {
    next()
  }
});

export default router
