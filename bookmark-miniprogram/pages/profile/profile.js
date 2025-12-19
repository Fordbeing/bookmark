// pages/profile/profile.js
const { getBookmarkListAPI } = require('../../api/bookmark');
const { getCategoryListAPI } = require('../../api/category');
const { logoutAPI } = require('../../api/auth');
const { showConfirm } = require('../../utils/util');

Page({
    data: {
        userInfo: {},
        stats: {
            bookmarkCount: 0,
            categoryCount: 0,
            favoriteCount: 0
        }
    },

    onLoad() {
        this.loadUserInfo();
    },

    onShow() {
        if (wx.getStorageSync('token')) {
            this.loadStats();
        }
    },

    // 加载用户信息
    loadUserInfo() {
        const userInfo = wx.getStorageSync('userInfo') || {};
        this.setData({ userInfo });

        // 检查登录状态
        if (!wx.getStorageSync('token')) {
            wx.navigateTo({ url: '/pages/login/login' });
        }
    },

    // 加载统计数据
    async loadStats() {
        try {
            const [bookmarkRes, categoryRes] = await Promise.all([
                getBookmarkListAPI({ page: 1, size: 1000 }),
                getCategoryListAPI()
            ]);

            const bookmarks = bookmarkRes.data?.list || [];
            const categories = categoryRes.data || [];

            this.setData({
                stats: {
                    bookmarkCount: bookmarks.length,
                    categoryCount: categories.length,
                    favoriteCount: bookmarks.filter(b => b.isFavorite === 1).length
                }
            });
        } catch (error) {
            console.error('加载统计失败:', error);
        }
    },

    // 跳转到首页
    goToIndex() {
        wx.switchTab({ url: '/pages/index/index' });
    },

    // 跳转到分类
    goToCategory() {
        wx.switchTab({ url: '/pages/category/category' });
    },

    // 跳转到回收站
    goToTrash() {
        wx.switchTab({ url: '/pages/trash/trash' });
    },

    // 清除缓存
    async clearCache() {
        const confirmed = await showConfirm('确认清除', '清除缓存后需要重新加载数据');
        if (!confirmed) return;

        try {
            // 保留登录信息
            const token = wx.getStorageSync('token');
            const userInfo = wx.getStorageSync('userInfo');

            wx.clearStorageSync();

            // 恢复登录信息
            if (token) wx.setStorageSync('token', token);
            if (userInfo) wx.setStorageSync('userInfo', userInfo);

            wx.showToast({ title: '缓存已清除', icon: 'success' });
        } catch (error) {
            console.error('清除缓存失败:', error);
        }
    },

    // 退出登录
    async handleLogout() {
        const confirmed = await showConfirm('确认退出', '确定要退出登录吗？');
        if (!confirmed) return;

        try {
            await logoutAPI();
        } catch (error) {
            // 忽略错误，继续清除本地登录状态
        }

        // 清除登录信息
        const app = getApp();
        app.clearLoginInfo();

        wx.showToast({ title: '已退出', icon: 'success' });

        setTimeout(() => {
            wx.reLaunch({ url: '/pages/login/login' });
        }, 1500);
    }
});
