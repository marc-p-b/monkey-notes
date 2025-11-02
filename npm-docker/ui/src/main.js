import "./assets/styles/main.css";

import { createApp } from "vue";
import PrimeVue from "primevue/config";
import App from "./App.vue";
import Aura from "@primeuix/themes/aura";
import router from './router'
import { createPinia } from 'pinia'
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
