import request from './request';

// ========== 链接健康检查 API ==========

/**
 * 触发链接健康检查
 */
export const triggerHealthCheckAPI = () => {
    return request.post('/bookmarks/health/check');
};

/**
 * 检测单个链接
 * @param {Number} id - 书签ID
 */
export const checkSingleLinkAPI = (id) => {
    return request.post(`/bookmarks/health/check/${id}`);
};

/**
 * 获取健康检查统计
 */
export const getHealthStatsAPI = () => {
    return request.get('/bookmarks/health/stats');
};
