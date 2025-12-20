// BookUtil API Service
const API_BASE_URL = 'http://115.159.219.125:88/api';

class BookUtilAPI {
    constructor() {
        this.token = null;
        this.loadToken();
    }

    // 从存储中加载 token
    async loadToken() {
        return new Promise((resolve) => {
            chrome.storage.local.get(['token'], (result) => {
                this.token = result.token || null;
                resolve(this.token);
            });
        });
    }

    // 保存 token 到存储
    async saveToken(token) {
        this.token = token;
        return new Promise((resolve) => {
            chrome.storage.local.set({ token }, resolve);
        });
    }

    // 清除 token
    async clearToken() {
        this.token = null;
        return new Promise((resolve) => {
            chrome.storage.local.remove(['token', 'user'], resolve);
        });
    }

    // 检查是否已登录
    isLoggedIn() {
        return !!this.token;
    }

    // 通用请求方法
    async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        try {
            const response = await fetch(url, {
                ...options,
                headers
            });

            const data = await response.json();

            if (data.code === 200) {
                return { success: true, data: data.data, message: data.message };
            } else {
                return { success: false, message: data.message || '操作失败' };
            }
        } catch (error) {
            console.error('API Error:', error);
            return { success: false, message: '网络连接失败，请检查网络' };
        }
    }

    // 登录
    async login(email, password) {
        const result = await this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });

        if (result.success && result.data) {
            await this.saveToken(result.data.token);
            await new Promise((resolve) => {
                chrome.storage.local.set({ user: result.data.user }, resolve);
            });
        }

        return result;
    }

    // 登出
    async logout() {
        await this.clearToken();
        return { success: true, message: '已退出登录' };
    }

    // 获取当前用户信息
    async getCurrentUser() {
        return this.request('/auth/me');
    }

    // 获取分类列表
    async getCategories() {
        return this.request('/categories');
    }

    // 创建书签
    async createBookmark(bookmark) {
        return this.request('/bookmarks', {
            method: 'POST',
            body: JSON.stringify(bookmark)
        });
    }

    // 获取书签列表
    async getBookmarks(page = 1, size = 10, categoryId = null) {
        let endpoint = `/bookmarks?page=${page}&size=${size}`;
        if (categoryId) {
            endpoint += `&categoryId=${categoryId}`;
        }
        return this.request(endpoint);
    }

    // 搜索书签
    async searchBookmarks(keyword) {
        return this.request(`/search?keyword=${encodeURIComponent(keyword)}`);
    }

    // 更新收藏状态
    async updateFavorite(id, isFavorite) {
        return this.request(`/bookmarks/${id}/favorite`, {
            method: 'PUT',
            body: JSON.stringify({ isFavorite })
        });
    }

    // 删除书签
    async deleteBookmark(id) {
        return this.request(`/bookmarks/${id}`, {
            method: 'DELETE'
        });
    }
}

// 创建全局实例
const api = new BookUtilAPI();
