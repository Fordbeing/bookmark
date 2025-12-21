import request from './request'

export const systemApi = {
    // 获取系统配置
    getConfig() {
        return request.get('/admin/system/config')
    },

    // 更新系统配置
    updateConfig(config) {
        return request.put('/admin/system/config', config)
    },

    // 设置维护模式
    setMaintenanceMode(enabled, message) {
        return request.post('/admin/system/maintenance', { enabled, message })
    }
}

export const announcementApi = {
    // 获取公告列表
    getList(params) {
        return request.get('/admin/system/announcements', { params })
    },

    // 创建公告
    create(data) {
        return request.post('/admin/system/announcements', data)
    },

    // 发布公告
    publish(id) {
        return request.post(`/admin/system/announcements/${id}/publish`)
    },

    // 更新公告
    update(id, data) {
        return request.put(`/admin/system/announcements/${id}`, data)
    },

    // 删除公告
    delete(id) {
        return request.delete(`/admin/system/announcements/${id}`)
    }
}
