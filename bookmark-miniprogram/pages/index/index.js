// pages/index/index.js
const { getBookmarkListAPI, createBookmarkAPI, deleteBookmarkAPI, updateBookmarkFavoriteAPI, searchBookmarksAPI } = require('../../api/bookmark');
const { getCategoryListAPI } = require('../../api/category');
const { formatDate, parseTags, copyToClipboard, showActionSheet, showConfirm } = require('../../utils/util');

Page({
    data: {
        bookmarks: [],
        filteredBookmarks: [],
        categories: [],
        loading: true,
        refreshing: false,
        addLoading: false,
        inputUrl: '',
        searchKeyword: '',
        currentFilter: 'all', // 'all', 'favorite', 'category'
        currentCategoryId: null,
        filterTitle: '我的书签',
        // 通知功能
        showNotifications: false,
        notifications: [
            {
                id: 1,
                icon: '🎉',
                title: '欢迎使用书签管理',
                content: '感谢您使用书签管理小程序，祝您使用愉快！',
                time: '刚刚',
                read: false
            },
            {
                id: 2,
                icon: '💡',
                title: '小技巧',
                content: '长按书签卡片可以快速进行操作哦',
                time: '1小时前',
                read: false
            },
            {
                id: 3,
                icon: '🔄',
                title: '系统更新',
                content: '我们优化了页面加载速度和交互体验',
                time: '昨天',
                read: true
            }
        ],
        unreadCount: 2
    },

    onLoad() {
        this.checkLoginAndLoad();
    },

    onShow() {
        // 每次显示页面时刷新数据
        if (wx.getStorageSync('token')) {
            this.loadData();

            // 检查是否有从其他页面传递的分类筛选
            const app = getApp();
            if (app.globalData.filterCategory) {
                const categoryId = app.globalData.filterCategory;
                app.globalData.filterCategory = null; // 清除标记

                // 延迟执行筛选，等待数据加载完成
                setTimeout(() => {
                    const category = this.data.categories.find(c => c.id === categoryId);
                    if (category) {
                        this.setData({
                            currentFilter: 'category',
                            currentCategoryId: categoryId,
                            filterTitle: `${category.icon || '📁'} ${category.name}`
                        });
                        this.applyFilter();
                    }
                }, 300);
            }
        }
    },

    // 检查登录状态
    checkLoginAndLoad() {
        const token = wx.getStorageSync('token');
        if (!token) {
            wx.navigateTo({ url: '/pages/login/login' });
            return;
        }
        this.loadData();
    },

    // 加载数据
    async loadData() {
        this.setData({ loading: true });

        try {
            // 并行加载书签和分类
            const [bookmarkRes, categoryRes] = await Promise.all([
                getBookmarkListAPI({ page: 1, size: 100 }),
                getCategoryListAPI()
            ]);

            let bookmarks = [];
            if (bookmarkRes.data && bookmarkRes.data.list) {
                bookmarks = bookmarkRes.data.list.map(item => ({
                    ...item,
                    parsedTags: parseTags(item.tags),
                    formattedDate: formatDate(item.createTime)
                }));
            }

            let categories = [];
            if (categoryRes.data) {
                categories = categoryRes.data.map(cat => ({
                    ...cat,
                    count: bookmarks.filter(b => b.categoryId === cat.id).length
                }));
            }

            this.setData({
                bookmarks,
                categories,
                loading: false,
                refreshing: false
            });

            this.applyFilter();
        } catch (error) {
            console.error('加载数据失败:', error);
            this.setData({ loading: false, refreshing: false });
        }
    },

    // 下拉刷新
    onRefresh() {
        this.setData({ refreshing: true });
        this.loadData();
    },

    // 应用筛选
    applyFilter() {
        const { bookmarks, currentFilter, currentCategoryId, searchKeyword } = this.data;
        let result = bookmarks;

        // 搜索筛选
        if (searchKeyword) {
            const keyword = searchKeyword.toLowerCase();
            result = result.filter(b =>
                (b.title && b.title.toLowerCase().includes(keyword)) ||
                (b.url && b.url.toLowerCase().includes(keyword)) ||
                (b.description && b.description.toLowerCase().includes(keyword))
            );
        }

        // 分类筛选
        if (currentFilter === 'favorite') {
            result = result.filter(b => b.isFavorite === 1);
        } else if (currentFilter === 'category' && currentCategoryId) {
            result = result.filter(b => b.categoryId === currentCategoryId);
        }

        // 按创建时间排序（最新在前）
        result = result.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));

        this.setData({ filteredBookmarks: result });
    },

    // 筛选：全部
    filterAll() {
        this.setData({
            currentFilter: 'all',
            currentCategoryId: null,
            filterTitle: '我的书签'
        });
        this.applyFilter();
    },

    // 筛选：收藏
    filterFavorite() {
        this.setData({
            currentFilter: 'favorite',
            currentCategoryId: null,
            filterTitle: '⭐ 收藏书签'
        });
        this.applyFilter();
    },

    // 筛选：分类
    filterByCategory(e) {
        const categoryId = e.currentTarget.dataset.id;
        const category = this.data.categories.find(c => c.id === categoryId);
        this.setData({
            currentFilter: 'category',
            currentCategoryId: categoryId,
            filterTitle: `${category?.icon || '📁'} ${category?.name || '分类'}`
        });
        this.applyFilter();
    },

    // 搜索输入
    onSearchInput(e) {
        this.setData({ searchKeyword: e.detail.value });
        this.applyFilter();
    },

    // 搜索
    handleSearch() {
        this.applyFilter();
    },

    // 清除搜索
    clearSearch() {
        this.setData({ searchKeyword: '' });
        this.applyFilter();
    },

    // URL 输入
    onUrlInput(e) {
        this.setData({ inputUrl: e.detail.value });
    },

    // 快速添加
    async handleQuickAdd() {
        let url = this.data.inputUrl.trim();
        if (!url) {
            wx.showToast({ title: '请输入网址', icon: 'none' });
            return;
        }

        if (!url.startsWith('http')) {
            url = 'http://' + url;
        }

        this.setData({ addLoading: true });

        try {
            await createBookmarkAPI({
                title: url.split('/')[2] || '新书签',
                url: url,
                description: '快速添加的书签'
            });

            wx.showToast({ title: '添加成功', icon: 'success' });
            this.setData({ inputUrl: '' });
            this.loadData();
        } catch (error) {
            console.error('添加失败:', error);
        } finally {
            this.setData({ addLoading: false });
        }
    },

    // 点击卡片
    handleCardTap(e) {
        const url = e.currentTarget.dataset.url;
        copyToClipboard(url);
    },

    // 复制链接
    copyUrl(e) {
        const url = e.currentTarget.dataset.url;
        copyToClipboard(url);
    },

    // 跳转编辑页
    goToEdit(e) {
        const id = e.currentTarget.dataset.id;
        wx.navigateTo({ url: `/pages/add/add?id=${id}` });
    },

    // 跳转添加页
    goToAddPage() {
        wx.navigateTo({ url: '/pages/add/add' });
    },

    // 长按显示操作菜单
    async showActionMenu(e) {
        const id = e.currentTarget.dataset.id;
        const bookmark = this.data.bookmarks.find(b => b.id === id);
        if (!bookmark) return;

        const actions = [
            '复制链接',
            '编辑书签',
            bookmark.isFavorite === 1 ? '取消收藏' : '添加收藏',
            '删除书签'
        ];

        const index = await showActionSheet(actions);

        switch (index) {
            case 0: // 复制链接
                copyToClipboard(bookmark.url);
                break;
            case 1: // 编辑
                wx.navigateTo({ url: `/pages/add/add?id=${id}` });
                break;
            case 2: // 收藏/取消收藏
                this.toggleFavorite(id, bookmark.isFavorite !== 1);
                break;
            case 3: // 删除
                this.deleteBookmark(id);
                break;
        }
    },

    // 切换收藏
    async toggleFavorite(id, isFavorite) {
        try {
            await updateBookmarkFavoriteAPI(id, isFavorite ? 1 : 0);
            wx.showToast({
                title: isFavorite ? '已收藏' : '已取消收藏',
                icon: 'success'
            });
            this.loadData();
        } catch (error) {
            console.error('操作失败:', error);
        }
    },

    // 删除书签
    async deleteBookmark(id) {
        const confirmed = await showConfirm('确认删除', '确定要删除这个书签吗？删除后可在回收站恢复。');
        if (!confirmed) return;

        try {
            await deleteBookmarkAPI(id);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadData();
        } catch (error) {
            console.error('删除失败:', error);
        }
    },

    // ========== 通知功能 ==========

    // 切换通知弹窗
    toggleNotifications() {
        this.setData({
            showNotifications: !this.data.showNotifications
        });
    },

    // 标记全部已读
    markAllAsRead() {
        const notifications = this.data.notifications.map(n => ({
            ...n,
            read: true
        }));
        this.setData({
            notifications,
            unreadCount: 0
        });
        wx.showToast({ title: '已全部标记为已读', icon: 'success' });
    },

    // 点击通知项
    handleNotificationClick(e) {
        const index = e.currentTarget.dataset.index;
        const notifications = [...this.data.notifications];
        if (!notifications[index].read) {
            notifications[index].read = true;
            const unreadCount = notifications.filter(n => !n.read).length;
            this.setData({ notifications, unreadCount });
        }
    }
});
