import request from './request'

export const userApi = {
    // 获取用户列表
    getList(params) {
        return request.get('/admin/users', { params })
    },

    // 获取用户详情
    getDetail(id) {
        return request.get(`/admin/users/${id}`)
    },

    // 修改用户状态
    updateStatus(id, status) {
        return request.put(`/admin/users/${id}/status`, { status })
    },

    // 设置管理员权限
    setAdmin(id, isAdmin) {
        return request.put(`/admin/users/${id}/admin`, { isAdmin })
    },

    // 调整用户配额
    updateQuota(id, data) {
        return request.put(`/admin/users/${id}/quota`, data)
    },

    // 重置密码
    resetPassword(id) {
        return request.post(`/admin/users/${id}/reset-pwd`)
    },

    // 删除用户
    delete(id) {
        return request.delete(`/admin/users/${id}`)
    },

    // 获取用户书签
    getBookmarks(id, params) {
        return request.get(`/admin/users/${id}/bookmarks`, { params })
    },

    // 获取用户激活记录
    getActivations(id) {
        return request.get(`/admin/users/${id}/activations`)
    },

    // 获取登录历史
    getLoginHistory(id, params) {
        return request.get(`/admin/users/${id}/login-history`, { params })
    }
}
