import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import Components from 'unplugin-vue-components/vite';
import { PrimeVueResolver } from '@primevue/auto-import-resolver';
import { fileURLToPath, URL } from 'node:url'


export default defineConfig(({ mode }) => ({
	plugins: [vue(),
		Components({
			resolvers: [
				PrimeVueResolver()
			]
		})
	],
	build: {
		sourcemap: mode === 'debug',       // ✅ generate source maps
		minify: mode === 'debug' ? false : 'esbuild', // ✅ no minify
	},
	define: {
		__DEBUG__: mode === 'debug',
	},resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url))
		},
	},
}))

