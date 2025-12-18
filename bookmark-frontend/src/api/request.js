import axios from 'axios';
import { ElMessage } from 'element-plus';

// 创建axios实例
const request = axios.create({
    baseURL: 'http://localhost:8080/api',
    timeout: 10000
});

// 请求拦截器：添加token
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        console.log('[Request]', config.method?.toUpperCase(), config.url);
        console.log('[Token from localStorage]', token ? `present (${token.substring(0, 30)}...)` : 'MISSING!');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
            console.log('[Auth Header Set]', 'Bearer ' + token.substring(0, 20) + '...');
        } else {
            console.warn('[WARNING] No token in localStorage!');
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
    error => {
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    ElMessage.error('未授权，请重新登录');
                    localStorage.removeItem('token');
                    localStorage.removeItem('user');
                    // 可以在这里跳转到登录页
                    break;
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
                    ElMessage.error(error.response.data.message || '请求失败');
            }
        } else {
            ElMessage.error('网络连接失败');
        }
        return Promise.reject(error);
    }
);

export default request;
