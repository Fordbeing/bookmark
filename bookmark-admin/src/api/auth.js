import request from './request'

export const authApi = {
    // 管理员登录
    adminLogin(data) {
        return request.post('/auth/admin-login', data)
    },

    // 获取当前用户信息
    getCurrentUser() {
        return request.get('/auth/me')
    }
}
