import request from './request';

/**
 * 用户注册
 * @param {Object} data - {username, email, password}
 */
export const registerAPI = (data) => {
    return request.post('/auth/register', data);
};

/**
 * 用户登录
 * @param {Object} data - {email, password, remember}
 */
export const loginAPI = (data) => {
    return request.post('/auth/login', data);
};

/**
 * 获取当前用户信息
 */
export const getCurrentUserAPI = () => {
    return request.get('/auth/me');
};

/**
 * 退出登录
 */
export const logoutAPI = () => {
    return request.post('/auth/logout');
};
