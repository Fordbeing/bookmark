import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
    baseURL: '/api',
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 是否正在刷新 Token
let isRefreshing = false
// 等待 Token 刷新的请求队列
let refreshSubscribers = []

// 订阅 Token 刷新
function subscribeTokenRefresh(callback) {
    refreshSubscribers.push(callback)
}

// 通知所有订阅者
function onRefreshed(token) {
    refreshSubscribers.forEach(callback => callback(token))
    refreshSubscribers = []
}

// 刷新 Access Token
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('admin_refresh_token')
    if (!refreshToken) {
        return null
    }

    try {
        const response = await axios.post('/api/auth/refresh', { refreshToken }, { timeout: 10000 })

        if (response.data.code === 200) {
            const { accessToken } = response.data.data
            localStorage.setItem('admin_token', accessToken)
            return accessToken
        }
    } catch (error) {
        console.error('Token 刷新失败:', error)
    }
    return null
}

// 请求拦截器
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('admin_token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        return response.data
    },
    async error => {
        const originalRequest = error.config

        if (error.response) {
            const { status, data } = error.response
            const errorCode = data?.error

            if (status === 401) {
                // 用户被禁用
                if (errorCode === 'USER_DISABLED') {
                    alert('账户已被禁用，请联系管理员')
                    clearAuthAndRedirect()
                    return Promise.reject(error)
                }

                // Token 被注销或过期，尝试刷新
                if (!originalRequest._retry) {
                    originalRequest._retry = true

                    if (!isRefreshing) {
                        isRefreshing = true
                        const newToken = await refreshAccessToken()
                        isRefreshing = false

                        if (newToken) {
                            onRefreshed(newToken)
                            originalRequest.headers.Authorization = `Bearer ${newToken}`
                            return request(originalRequest)
                        } else {
                            clearAuthAndRedirect()
                            return Promise.reject(error)
                        }
                    } else {
                        return new Promise(resolve => {
                            subscribeTokenRefresh(token => {
                                originalRequest.headers.Authorization = `Bearer ${token}`
                                resolve(request(originalRequest))
                            })
                        })
                    }
                }

                clearAuthAndRedirect()
            } else if (status === 403) {
                // 403 表示权限不足，不应跳转登录页，让组件自行处理
                console.error('权限不足:', data?.message || '无权访问此资源')
            }
        }
        return Promise.reject(error)
    }
)

// 清除认证并跳转
function clearAuthAndRedirect() {
    const authStore = useAuthStore()
    authStore.logout()
    router.push('/login')
}

export default request
