// api/auth.js
// 用户认证相关 API

const { post, get } = require('../utils/request');

/**
 * 用户注册
 * @param {Object} data - {username, email, password}
 */
const registerAPI = (data) => {
    return post('/auth/register', data);
};

/**
 * 用户登录
 * @param {Object} data - {email, password, remember}
 */
const loginAPI = (data) => {
    return post('/auth/login', data);
};

/**
 * 微信登录
 * @param {Object} data - {code, nickName, avatarUrl}
 */
const wxLoginAPI = (data) => {
    return post('/auth/wx-login', data);
};

/**
 * 绑定手机号
 * @param {Object} data - {phone, code}
 */
const bindPhoneAPI = (data) => {
    return post('/auth/bind-phone', data);
};

/**
 * 获取当前用户信息
 */
const getCurrentUserAPI = () => {
    return get('/auth/me');
};

/**
 * 退出登录
 */
const logoutAPI = () => {
    return post('/auth/logout');
};

module.exports = {
    registerAPI,
    loginAPI,
    wxLoginAPI,
    bindPhoneAPI,
    getCurrentUserAPI,
    logoutAPI
};
