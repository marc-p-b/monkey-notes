import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
    state: () => ({
        loading: false,
        search: ''
    }),
    actions: {
        setLoading(value) {
            this.loading = value
        },
        setSearch(value) {
            this.search = value
        }
    }
})