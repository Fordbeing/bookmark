import request from './request'

export const activationApi = {
    // 获取激活码列表
    getList(params) {
        return request.get('/admin/activation-codes', { params })
    },

    // 创建单个激活码
    create(data) {
        return request.post('/admin/activation-codes', data)
    },

    // 批量创建激活码
    batchCreate(data) {
        return request.post('/admin/activation-codes/batch', data)
    },

    // 修改激活码
    update(id, data) {
        return request.put(`/admin/activation-codes/${id}`, data)
    },

    // 删除激活码
    delete(id) {
        return request.delete(`/admin/activation-codes/${id}`)
    },

    // 获取使用记录
    getUsage(id) {
        return request.get(`/admin/activation-codes/${id}/usage`)
    },

    // 获取用户激活记录
    getUserActivations(params) {
        return request.get('/admin/user-activations', { params })
    },

    // 修改用户激活记录
    updateUserActivation(id, data) {
        return request.put(`/admin/user-activations/${id}`, data)
    }
}
