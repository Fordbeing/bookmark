// pages/trash/trash.js
const { getTrashBookmarksAPI, restoreBookmarkAPI, permanentDeleteBookmarkAPI, clearTrashAPI } = require('../../api/bookmark');
const { showConfirm } = require('../../utils/util');

Page({
    data: {
        bookmarks: [],
        loading: true,
        refreshing: false
    },

    onLoad() {
        this.checkLoginAndLoad();
    },

    onShow() {
        if (wx.getStorageSync('token')) {
            this.loadTrash();
        }
    },

    // 检查登录状态
    checkLoginAndLoad() {
        const token = wx.getStorageSync('token');
        if (!token) {
            wx.navigateTo({ url: '/pages/login/login' });
            return;
        }
        this.loadTrash();
    },

    // 加载回收站
    async loadTrash() {
        this.setData({ loading: true });

        try {
            const res = await getTrashBookmarksAPI();
            if (res.data) {
                this.setData({ bookmarks: res.data });
            }
        } catch (error) {
            console.error('加载回收站失败:', error);
        } finally {
            this.setData({ loading: false, refreshing: false });
        }
    },

    // 下拉刷新
    onRefresh() {
        this.setData({ refreshing: true });
        this.loadTrash();
    },

    // 恢复书签
    async restoreBookmark(e) {
        const id = e.currentTarget.dataset.id;

        try {
            await restoreBookmarkAPI(id);
            wx.showToast({ title: '已恢复', icon: 'success' });
            this.loadTrash();
        } catch (error) {
            console.error('恢复失败:', error);
        }
    },

    // 永久删除
    async permanentDelete(e) {
        const id = e.currentTarget.dataset.id;
        const confirmed = await showConfirm('确认永久删除', '永久删除后无法恢复，确定要删除吗？');
        if (!confirmed) return;

        try {
            await permanentDeleteBookmarkAPI(id);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadTrash();
        } catch (error) {
            console.error('删除失败:', error);
        }
    },

    // 清空回收站
    async clearTrash() {
        const confirmed = await showConfirm('确认清空', '清空回收站后所有书签将永久删除，无法恢复！');
        if (!confirmed) return;

        try {
            await clearTrashAPI();
            wx.showToast({ title: '已清空', icon: 'success' });
            this.setData({ bookmarks: [] });
        } catch (error) {
            console.error('清空失败:', error);
        }
    }
});
