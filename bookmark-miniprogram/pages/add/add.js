// pages/add/add.js
const { getBookmarkByIdAPI, createBookmarkAPI, updateBookmarkAPI, deleteBookmarkAPI } = require('../../api/bookmark');
const { getCategoryListAPI } = require('../../api/category');
const { showConfirm, parseTags } = require('../../utils/util');

Page({
    data: {
        isEdit: false,
        bookmarkId: null,
        loading: false,
        form: {
            url: '',
            title: '',
            description: '',
            categoryId: null,
            tags: [],
            isFavorite: false
        },
        tagInput: '',
        categories: [],
        categoryOptions: [{ id: null, name: '无分类' }],
        selectedCategoryIndex: 0,
        selectedCategoryName: '无分类'
    },

    onLoad(options) {
        if (options.id) {
            this.setData({
                isEdit: true,
                bookmarkId: parseInt(options.id)
            });
            this.loadBookmark(options.id);
        }
        this.loadCategories();
    },

    // 加载书签详情
    async loadBookmark(id) {
        try {
            const res = await getBookmarkByIdAPI(id);
            if (res.data) {
                const bookmark = res.data;
                const tags = parseTags(bookmark.tags);
                this.setData({
                    'form.url': bookmark.url || '',
                    'form.title': bookmark.title || '',
                    'form.description': bookmark.description || '',
                    'form.categoryId': bookmark.categoryId,
                    'form.tags': tags,
                    'form.isFavorite': bookmark.isFavorite === 1
                });

                // 更新分类选择
                this.updateCategorySelection(bookmark.categoryId);
            }
        } catch (error) {
            console.error('加载书签失败:', error);
            wx.showToast({ title: '加载失败', icon: 'none' });
        }
    },

    // 加载分类列表
    async loadCategories() {
        try {
            const res = await getCategoryListAPI();
            if (res.data) {
                const categories = res.data;
                const categoryOptions = [
                    { id: null, name: '无分类' },
                    ...categories
                ];
                this.setData({ categories, categoryOptions });

                // 更新分类选择
                if (this.data.form.categoryId) {
                    this.updateCategorySelection(this.data.form.categoryId);
                }
            }
        } catch (error) {
            console.error('加载分类失败:', error);
        }
    },

    // 更新分类选择
    updateCategorySelection(categoryId) {
        const { categoryOptions } = this.data;
        const index = categoryOptions.findIndex(c => c.id === categoryId);
        if (index >= 0) {
            this.setData({
                selectedCategoryIndex: index,
                selectedCategoryName: categoryOptions[index].name,
                'form.categoryId': categoryId
            });
        }
    },

    // 表单输入
    onUrlInput(e) {
        this.setData({ 'form.url': e.detail.value });
    },

    onTitleInput(e) {
        this.setData({ 'form.title': e.detail.value });
    },

    onDescriptionInput(e) {
        this.setData({ 'form.description': e.detail.value });
    },

    onCategoryChange(e) {
        const index = e.detail.value;
        const category = this.data.categoryOptions[index];
        this.setData({
            selectedCategoryIndex: index,
            selectedCategoryName: category.name,
            'form.categoryId': category.id
        });
    },

    onTagInput(e) {
        this.setData({ tagInput: e.detail.value });
    },

    addTag() {
        const tag = this.data.tagInput.trim();
        if (tag && !this.data.form.tags.includes(tag)) {
            this.setData({
                'form.tags': [...this.data.form.tags, tag],
                tagInput: ''
            });
        }
    },

    removeTag(e) {
        const index = e.currentTarget.dataset.index;
        const tags = [...this.data.form.tags];
        tags.splice(index, 1);
        this.setData({ 'form.tags': tags });
    },

    onFavoriteChange(e) {
        this.setData({ 'form.isFavorite': e.detail.value });
    },

    // 提交
    async handleSubmit() {
        const { form, isEdit, bookmarkId } = this.data;

        // 验证
        if (!form.url) {
            wx.showToast({ title: '请输入网址', icon: 'none' });
            return;
        }

        if (!form.title) {
            wx.showToast({ title: '请输入标题', icon: 'none' });
            return;
        }

        let url = form.url;
        if (!url.startsWith('http')) {
            url = 'http://' + url;
        }

        this.setData({ loading: true });

        try {
            const data = {
                url: url,
                title: form.title,
                description: form.description,
                categoryId: form.categoryId,
                tags: JSON.stringify(form.tags),
                isFavorite: form.isFavorite ? 1 : 0
            };

            if (isEdit) {
                await updateBookmarkAPI(bookmarkId, data);
                wx.showToast({ title: '保存成功', icon: 'success' });
            } else {
                await createBookmarkAPI(data);
                wx.showToast({ title: '添加成功', icon: 'success' });
            }

            setTimeout(() => {
                wx.navigateBack();
            }, 1500);
        } catch (error) {
            console.error('保存失败:', error);
        } finally {
            this.setData({ loading: false });
        }
    },

    // 删除
    async handleDelete() {
        const confirmed = await showConfirm('确认删除', '确定要删除这个书签吗？');
        if (!confirmed) return;

        try {
            await deleteBookmarkAPI(this.data.bookmarkId);
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => {
                wx.navigateBack();
            }, 1500);
        } catch (error) {
            console.error('删除失败:', error);
        }
    },

    // 返回
    goBack() {
        wx.navigateBack();
    }
});
