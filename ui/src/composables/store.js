import { defineStore } from 'pinia'
import { isTokenValid } from '@/router/index'
import { authFetch } from '@/requests'

export const useUiStore = defineStore('ui', {
    state: () => ({
        loading: false,
        search: '',
        srPages: [],
        isConnected: isTokenValid(localStorage.getItem("token")),
        userData: null,
        //bumped when Home is clicked while already on Home: router.push('/') is a no-op there, so
        //there is no navigation for the view to react to. Home watches this counter instead.
        homeRefreshKey: 0
    }),
    actions: {
        setLoading(value) {
            this.loading = value
        },
        refreshHome() {
            this.homeRefreshKey++
        },
        setSearch(value) {
            this.search = value
        },
        setSRPages(value) {
            this.srPages = value
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