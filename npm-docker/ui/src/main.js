import "./assets/styles/main.css";
import "./assets/styles/mn.css";

import { createApp } from "vue";
import PrimeVue from "primevue/config";
import App from "./App.vue";
import Aura from "@primeuix/themes/aura";
import router from './router'
import { createPinia } from 'pinia'

// this to unsure env.js is loaded ! - maybe not so useful (see index.html and env.js)
// env.js MUST be loaded before anything else (unless dist version wont work properly)
// const waitForEnv = () =>
// 	new Promise(resolve => {
// 		const check = () => window._env_ ? resolve() : setTimeout(check, 10)
// 		check()
// 	})
// await waitForEnv()
// done

const app = createApp(App);

app.use(PrimeVue, {
	theme: {
		preset: Aura,
		options: {
			darkModeSelector: ".p-dark",
		}
	},
});
app.use(createPinia())
app.use(router)
app.mount("#app");
