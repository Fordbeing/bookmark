// pages/category/category.js
const { getCategoryListAPI, createCategoryAPI, updateCategoryAPI, deleteCategoryAPI } = require('../../api/category');
const { getBookmarkListAPI } = require('../../api/bookmark');
const { showConfirm } = require('../../utils/util');

Page({
    data: {
        categories: [],
        loading: true,
        showModal: false,
        saving: false,
        editingId: null,
        modalForm: {
            name: '',
            icon: '📁'
        },
        iconOptions: ['📁', '💼', '🎮', '📚', '🎵', '🎬', '📷', '💻', '🔧', '📝', '🌐', '🛒', '✈️', '🏠', '❤️', '⭐']
    },

    onLoad() {
        this.checkLoginAndLoad();
    },

    onShow() {
        if (wx.getStorageSync('token')) {
            this.loadCategories();
        }
    },

    // 检查登录状态
    checkLoginAndLoad() {
        const token = wx.getStorageSync('token');
        if (!token) {
            wx.navigateTo({ url: '/pages/login/login' });
            return;
        }
        this.loadCategories();
    },

    // 加载分类
    async loadCategories() {
        this.setData({ loading: true });

        try {
            // 加载分类和书签
            const [categoryRes, bookmarkRes] = await Promise.all([
                getCategoryListAPI(),
                getBookmarkListAPI({ page: 1, size: 100 })
            ]);

            let categories = [];
            if (categoryRes.data) {
                const bookmarks = bookmarkRes.data?.list || [];
                categories = categoryRes.data.map(cat => ({
                    ...cat,
                    count: bookmarks.filter(b => b.categoryId === cat.id).length
                }));
            }

            this.setData({ categories, loading: false });
        } catch (error) {
            console.error('加载分类失败:', error);
            this.setData({ loading: false });
        }
    },

    // 显示添加弹窗
    showAddModal() {
        this.setData({
            showModal: true,
            editingId: null,
            modalForm: { name: '', icon: '📁' }
        });
    },

    // 编辑分类
    editCategory(e) {
        const id = e.currentTarget.dataset.id;
        const category = this.data.categories.find(c => c.id === id);
        if (category) {
            this.setData({
                showModal: true,
                editingId: id,
                modalForm: {
                    name: category.name,
                    icon: category.icon || '📁'
                }
            });
        }
    },

    // 隐藏弹窗
    hideModal() {
        this.setData({ showModal: false });
    },

    preventBubble() { },

    // 表单输入
    onModalNameInput(e) {
        this.setData({ 'modalForm.name': e.detail.value });
    },

    selectIcon(e) {
        const icon = e.currentTarget.dataset.icon;
        this.setData({ 'modalForm.icon': icon });
    },

    // 保存分类
    async saveCategory() {
        const { modalForm, editingId } = this.data;

        if (!modalForm.name.trim()) {
            wx.showToast({ title: '请输入分类名称', icon: 'none' });
            return;
        }

        this.setData({ saving: true });

        try {
            if (editingId) {
                await updateCategoryAPI(editingId, modalForm);
                wx.showToast({ title: '保存成功', icon: 'success' });
            } else {
                await createCategoryAPI(modalForm);
                wx.showToast({ title: '创建成功', icon: 'success' });
            }

            this.setData({ showModal: false });
            this.loadCategories();
        } catch (error) {
            console.error('保存失败:', error);
        } finally {
            this.setData({ saving: false });
        }
    },

    // 删除分类
    async deleteCategory(e) {
        const id = e.currentTarget.dataset.id;
        const confirmed = await showConfirm('确认删除', '删除分类后，该分类下的书签将变为"无分类"。');
        if (!confirmed) return;

        try {
            await deleteCategoryAPI(id);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadCategories();
        } catch (error) {
            console.error('删除失败:', error);
        }
    },

    // 跳转到分类书签
    goToCategoryBookmarks(e) {
        const id = e.currentTarget.dataset.id;
        // 跳转到首页并传递分类ID
        wx.switchTab({
            url: '/pages/index/index',
            success: () => {
                // 通过全局事件传递筛选条件
                const app = getApp();
                app.globalData.filterCategory = id;
            }
        });
    }
});
