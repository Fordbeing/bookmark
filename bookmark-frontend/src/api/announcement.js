import request from './request';

/**
 * 公告 API
 */

// 获取已发布公告列表
export const getAnnouncementsAPI = (limit = 10) => {
    return request({
        url: '/announcements',
        method: 'GET',
        params: { limit }
    });
};

// 获取公告详情
export const getAnnouncementDetailAPI = (id) => {
    return request({
        url: `/announcements/${id}`,
        method: 'GET'
    });
};
