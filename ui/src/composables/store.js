import { defineStore } from 'pinia'
import { isTokenValid } from '@/router/index'
import { authFetch } from '@/requests'

export const useUiStore = defineStore('ui', {
    state: () => ({
        loading: false,
        transcript_edit_mode: false,
        search: '',
        srPages: [],
        isConnected: isTokenValid(localStorage.getItem("token")),
        userData: null
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
        },
        async fetchCurrentUser() {
            try {
                const response = await authFetch("user/whoami")
                if (!response.ok) throw new Error("Network response was not ok")

                this.userData = await response.json()
            } catch (err) {
                console.error(err)
            }
        },
        clearCurrentUser() {
            this.userData = null
        }
    }
})