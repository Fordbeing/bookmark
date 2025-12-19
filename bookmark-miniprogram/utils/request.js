// utils/request.js
// 封装 wx.request 为 Promise，添加 JWT token 拦截

const app = getApp();

const request = (options) => {
    return new Promise((resolve, reject) => {
        const token = wx.getStorageSync('token');
        const baseUrl = app.globalData.baseUrl || 'http://localhost:8080/api';

        // 构建完整 URL
        const url = options.url.startsWith('http') ? options.url : baseUrl + options.url;

        // 构建请求头
        const header = {
            'Content-Type': 'application/json',
            ...options.header
        };

        // 添加 token
        if (token) {
            header['Authorization'] = `Bearer ${token}`;
        }

        wx.request({
            url: url,
            method: options.method || 'GET',
            data: options.data,
            header: header,
            timeout: options.timeout || 10000,
            success: (res) => {
                const data = res.data;

                // 处理业务响应码
                if (data.code === 200) {
                    resolve(data);
                } else if (res.statusCode === 401 || data.code === 401) {
                    // 未授权，清除登录信息并跳转
                    wx.removeStorageSync('token');
                    wx.removeStorageSync('userInfo');
                    wx.showToast({
                        title: '请重新登录',
                        icon: 'none'
                    });
                    setTimeout(() => {
                        wx.navigateTo({
                            url: '/pages/login/login'
                        });
                    }, 1500);
                    reject({ code: 401, message: '未授权' });
                } else {
                    wx.showToast({
                        title: data.message || '请求失败',
                        icon: 'none'
                    });
                    reject(data);
                }
            },
            fail: (err) => {
                console.error('请求失败:', err);
                wx.showToast({
                    title: '网络连接失败',
                    icon: 'none'
                });
                reject(err);
            }
        });
    });
};

// 便捷方法
const get = (url, params) => {
    return request({
        url: url,
        method: 'GET',
        data: params
    });
};

const post = (url, data) => {
    return request({
        url: url,
        method: 'POST',
        data: data
    });
};

const put = (url, data) => {
    return request({
        url: url,
        method: 'PUT',
        data: data
    });
};

const del = (url, data) => {
    return request({
        url: url,
        method: 'DELETE',
        data: data
    });
};

module.exports = {
    request,
    get,
    post,
    put,
    del
};
