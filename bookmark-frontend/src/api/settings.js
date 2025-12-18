import request from './request';

/**
 * 获取用户设置
 */
export const getSettingsAPI = () => {
    return request.get('/settings');
};

/**
 * 更新用户设置
 * @param {Object} data - 设置对象
 */
export const updateSettingsAPI = (data) => {
    return request.put('/settings', data);
};
