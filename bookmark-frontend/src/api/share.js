import request from './request';

// ========== 分享功能 API ==========

/**
 * 创建分享
 * @param {Object} data - {categoryId, password, expireDays}
 */
export const createShareAPI = (data) => {
    return request.post('/share/create', data);
};

/**
 * 取消分享
 * @param {Number} id - 分享ID
 */
export const cancelShareAPI = (id) => {
    return request.delete(`/share/${id}`);
};

/**
 * 获取我的分享列表
 */
export const getMySharesAPI = () => {
    return request.get('/share/my-shares');
};

// ========== 公开访问 API (无需登录) ==========

/**
 * 检查分享是否需要密码
 * @param {String} code - 分享码
 */
export const checkShareAPI = (code) => {
    return request.get(`/public/share/${code}/check`);
};

/**
 * 获取分享内容
 * @param {String} code - 分享码
 * @param {String} password - 密码(可选)
 */
export const getShareContentAPI = (code, password) => {
    return request.get(`/public/share/${code}`, { params: { password } });
};
