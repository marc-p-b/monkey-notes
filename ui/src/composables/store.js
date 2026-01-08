import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
    state: () => ({
        loading: false,
        transcript_edit_mode: false,
        search: '',
        srPages: []
    }),
    actions: {
        setLoading(value) {
            this.loading = value
        },
        setSearch(value) {
            this.search = value
        },
        setSRPages(value) {
            this.srPages = value
        },
        transcriptEditMode() {
            this.transcript_edit_mode = true
        },
        transcriptViewMode() {
            this.transcript_edit_mode = false
        }
    }
})