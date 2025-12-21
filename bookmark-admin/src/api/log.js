import request from './request'

export const logApi = {
    // 获取操作日志列表
    getList(params) {
        return request.get('/admin/logs', { params })
    },

    // 获取日志详情
    getDetail(id) {
        return request.get(`/admin/logs/${id}`)
    },

    // 导出日志
    exportLogs(params) {
        return request.get('/admin/logs/export', {
            params,
            responseType: 'blob'
        })
    }
}
