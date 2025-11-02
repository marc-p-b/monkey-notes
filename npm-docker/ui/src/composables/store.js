import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
    state: () => ({
        loading: false
    }),
    actions: {
        setLoading(value) {
            this.loading = value
        }
    }
})