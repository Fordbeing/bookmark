import request from './request'

export const dashboardApi = {
    // 获取系统概览数据
    getOverview() {
        return request.get('/admin/dashboard/overview')
    },

    // 获取趋势数据
    getTrends(days = 7) {
        return request.get('/admin/dashboard/trends', { params: { days } })
    },

    // 获取 Elasticsearch 状态
    getElasticsearchStatus() {
        return request.get('/admin/dashboard/elasticsearch')
    },

    // 获取最近活动
    getRecentActivities(limit = 10) {
        return request.get('/admin/dashboard/activities', { params: { limit } })
    }
}
