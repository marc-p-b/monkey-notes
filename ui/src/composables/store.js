import { defineStore } from 'pinia'
import { isTokenValid } from '@/router/index'

export const useUiStore = defineStore('ui', {
    state: () => ({
        loading: false,
        transcript_edit_mode: false,
        search: '',
        srPages: [],
        isConnected: isTokenValid(localStorage.getItem("token"))
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
        },
        refreshAuth() {
            this.isConnected = isTokenValid(localStorage.getItem("token"))
        }
    }
})