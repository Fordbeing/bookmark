import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem('admin_token') || '')
    const refreshToken = ref(localStorage.getItem('admin_refresh_token') || '')
    const user = ref(JSON.parse(localStorage.getItem('admin_user') || 'null'))

    const isLoggedIn = computed(() => !!token.value && user.value?.isAdmin === 1)
    const isAdmin = computed(() => user.value?.isAdmin === 1)

    async function login(credentials) {
        const response = await authApi.adminLogin(credentials)
        if (response.code === 200) {
            const data = response.data
            // 优先使用新版双 Token，兼容旧版
            const accessToken = data.accessToken || data.token
            const refToken = data.refreshToken || ''

            token.value = accessToken
            refreshToken.value = refToken

            user.value = {
                id: data.id,
                username: data.username,
                email: data.email,
                nickname: data.nickname,
                avatar: data.avatar,
                isAdmin: data.isAdmin
            }

            // 存储到 localStorage
            localStorage.setItem('admin_token', accessToken)
            if (refToken) {
                localStorage.setItem('admin_refresh_token', refToken)
            }
            localStorage.setItem('admin_user', JSON.stringify(user.value))

            return { success: true }
        }
        return { success: false, message: response.message }
    }

    function logout() {
        token.value = ''
        refreshToken.value = ''
        user.value = null
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_refresh_token')
        localStorage.removeItem('admin_user')
    }

    function updateUser(userData) {
        user.value = userData
        localStorage.setItem('admin_user', JSON.stringify(userData))
    }

    return {
        token,
        refreshToken,
        user,
        isLoggedIn,
        isAdmin,
        login,
        logout,
        updateUser
    }
})
