import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../components/Home.vue'
import LoginView from '../components/Login.vue'
import PreferencesView from "../components/Preferences.vue";
import TranscriptView from "@/components/TranscriptView.vue";
import AgentView from "@/components/AgentView.vue";
import ProcessesView from "@/components/ProcessesView.vue";
import NamedEntitiesView from "@/components/NamedEntitiesView.vue";
import SearchView from "@/components/SearchView.vue";
import UsersView from "@/components/UsersView.vue";
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
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/transcript/:fileId',
      name: 'transcript',
      component: TranscriptView,
      props: true,
    },
    {
      path: '/transcript/:fileId',
      name: 'transcriptSearchResult',
      component: TranscriptView,
      props: true,
    },
    {
      path: '/agent/:fileId',
      name: 'agent',
      component: AgentView,
      props: true,
    },
    {
      path: '/preferences',
      name: 'prefs',
      component: PreferencesView,
    },
    {
      path: '/processes',
      name: 'processes',
      component: ProcessesView,
    },
    {
      path: '/ne',
      name: 'ne',
      component: NamedEntitiesView,
    },
    {
      path: '/search',
      name: 'search',
      component: SearchView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/users',
      name: 'users',
      component: UsersView,
    }
  ],
})

router.beforeEach((to, from, next) => {

  if (isTokenValid(localStorage.getItem("token")) && to.path === "/logout") {
    //console.log("logout !")
    localStorage.removeItem("token");
    next("/login");
  } else if (!isTokenValid(localStorage.getItem("token")) && to.path !== "/login") {
    //console.log("token is invalid, req is " + to.path)

    next("/login");
  } else {
    next()
  }
});

export default router
