// api/bookmark.js
// 书签相关 API

const { get, post, put, del } = require('../utils/request');

/**
 * 获取书签列表
 * @param {Object} params - {page, size, categoryId, keyword, sortBy, sortOrder}
 */
const getBookmarkListAPI = (params) => {
    return get('/bookmarks', params);
};

/**
 * 获取书签详情
 * @param {Number} id - 书签ID
 */
const getBookmarkByIdAPI = (id) => {
    return get(`/bookmarks/${id}`);
};

/**
 * 创建书签
 * @param {Object} data - {title, url, description, categoryId, tags, isFavorite}
 */
const createBookmarkAPI = (data) => {
    return post('/bookmarks', data);
};

/**
 * 更新书签
 * @param {Number} id - 书签ID
 * @param {Object} data - {title, description, categoryId, tags, isFavorite}
 */
const updateBookmarkAPI = (id, data) => {
    return put(`/bookmarks/${id}`, data);
};

/**
 * 删除书签（移入回收站）
 * @param {Number} id - 书签ID
 */
const deleteBookmarkAPI = (id) => {
    return del(`/bookmarks/${id}`);
};

/**
 * 批量删除书签
 * @param {Array} ids - 书签ID数组
 */
const batchDeleteBookmarksAPI = (ids) => {
    return del('/bookmarks/batch', { ids });
};

/**
 * 收藏/取消收藏书签
 * @param {Number} id - 书签ID
 * @param {Number} isFavorite - 0或1
 */
const updateBookmarkFavoriteAPI = (id, isFavorite) => {
    return put(`/bookmarks/${id}/favorite`, { isFavorite });
};

/**
 * 记录书签访问
 * @param {Number} id - 书签ID
 */
const recordBookmarkVisitAPI = (id) => {
    return put(`/bookmarks/${id}/visit`);
};

// ========== 回收站相关 API ==========

/**
 * 获取回收站书签
 */
const getTrashBookmarksAPI = () => {
    return get('/bookmarks/trash');
};

/**
 * 恢复书签
 * @param {Number} id - 书签ID
 */
const restoreBookmarkAPI = (id) => {
    return put(`/bookmarks/${id}/restore`);
};

/**
 * 永久删除书签
 * @param {Number} id - 书签ID
 */
const permanentDeleteBookmarkAPI = (id) => {
    return del(`/bookmarks/${id}/permanent`);
};

/**
 * 清空回收站
 */
const clearTrashAPI = () => {
    return del('/bookmarks/trash/clear');
};

// ========== 搜索相关 API ==========

/**
 * 搜索书签
 * @param {string} keyword - 搜索关键词
 */
const searchBookmarksAPI = (keyword) => {
    return get('/search', { keyword });
};

module.exports = {
    getBookmarkListAPI,
    getBookmarkByIdAPI,
    createBookmarkAPI,
    updateBookmarkAPI,
    deleteBookmarkAPI,
    batchDeleteBookmarksAPI,
    updateBookmarkFavoriteAPI,
    recordBookmarkVisitAPI,
    getTrashBookmarksAPI,
    restoreBookmarkAPI,
    permanentDeleteBookmarkAPI,
    clearTrashAPI,
    searchBookmarksAPI
};
