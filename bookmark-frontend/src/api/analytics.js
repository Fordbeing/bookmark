import request from './request';

// ========== 用户统计分析 API ==========

/**
 * 获取统计概览
 */
export const getAnalyticsOverviewAPI = () => {
    return request.get('/analytics/overview');
};

/**
 * 获取域名分布
 */
export const getDomainDistributionAPI = () => {
    return request.get('/analytics/domain-distribution');
};

/**
 * 获取时间线统计
 */
export const getTimelineAPI = () => {
    return request.get('/analytics/timeline');
};

/**
 * 获取常访问书签TOP10
 */
export const getTopVisitedAPI = () => {
    return request.get('/analytics/top-visited');
};
