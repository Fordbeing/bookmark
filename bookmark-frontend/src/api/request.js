import axios from 'axios';
import { ElMessage } from 'element-plus';

// 根据环境自动选择 API 地址
const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000
});

// 是否正在刷新 Token
let isRefreshing = false;
// 等待 Token 刷新的请求队列
let refreshSubscribers = [];

// 订阅 Token 刷新
function subscribeTokenRefresh(callback) {
    refreshSubscribers.push(callback);
}

// 通知所有订阅者
function onRefreshed(token) {
    refreshSubscribers.forEach(callback => callback(token));
    refreshSubscribers = [];
}

// 刷新 Access Token
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
        return null;
    }

    try {
        const response = await axios.post(
            (import.meta.env.VITE_API_BASE_URL || '/api') + '/auth/refresh',
            { refreshToken },
            { timeout: 10000 }
        );

        if (response.data.code === 200) {
            const { accessToken } = response.data.data;
            localStorage.setItem('token', accessToken);
            localStorage.setItem('accessToken', accessToken);
            return accessToken;
        }
    } catch (error) {
        console.error('Token 刷新失败:', error);
    }
    return null;
}

// 请求拦截器：添加 Token
request.interceptors.request.use(
    config => {
        // 优先使用 accessToken，兼容旧版 token
        const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器：统一处理响应
request.interceptors.response.use(
    response => {
        const result = response.data;
        if (result.code === 200) {
            return result;
        } else {
            ElMessage.error(result.message || '操作失败');
            return Promise.reject(result);
        }
    },
    async error => {
        const originalRequest = error.config;

        if (error.response) {
            const { status, data } = error.response;
            const errorCode = data?.error;

            // 处理 401 错误
            if (status === 401) {
                // 用户被禁用
                if (errorCode === 'USER_DISABLED') {
                    ElMessage.error('账户已被禁用，请联系管理员');
                    clearAuthAndRedirect();
                    return Promise.reject(error);
                }

                // Token 被注销或过期
                if (errorCode === 'TOKEN_REVOKED' || errorCode === 'INVALID_TOKEN') {
                    // 尝试使用 Refresh Token 刷新
                    if (!originalRequest._retry) {
                        originalRequest._retry = true;

                        if (!isRefreshing) {
                            isRefreshing = true;
                            const newToken = await refreshAccessToken();
                            isRefreshing = false;

                            if (newToken) {
                                onRefreshed(newToken);
                                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                                return request(originalRequest);
                            } else {
                                // 刷新失败，清除登录状态
                                ElMessage.error('登录已过期，请重新登录');
                                clearAuthAndRedirect();
                                return Promise.reject(error);
                            }
                        } else {
                            // 正在刷新，等待刷新完成
                            return new Promise(resolve => {
                                subscribeTokenRefresh(token => {
                                    originalRequest.headers.Authorization = `Bearer ${token}`;
                                    resolve(request(originalRequest));
                                });
                            });
                        }
                    }
                }

                // 其他 401 错误
                ElMessage.error(data?.message || '未授权，请重新登录');
                clearAuthAndRedirect();
                return Promise.reject(error);
            }

            switch (status) {
                case 403:
                    ElMessage.error('拒绝访问');
                    break;
                case 404:
                    ElMessage.error('请求的资源不存在');
                    break;
                case 500:
                    ElMessage.error('服务器错误');
                    break;
                default:
                    ElMessage.error(data?.message || '请求失败');
            }
        } else {
            ElMessage.error('网络连接失败');
        }
        return Promise.reject(error);
    }
);

// 清除认证信息并跳转到登录页
function clearAuthAndRedirect() {
    localStorage.removeItem('token');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');

    // 使用 window.location 跳转到登录页
    if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
    }
}

export default request;
