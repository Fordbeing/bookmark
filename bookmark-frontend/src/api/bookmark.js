import request from './request';

/**
 * 获取书签列表
 * @param {Object} params - {page, size, categoryId, keyword, sortBy, sortOrder}
 */
export const getBookmarkListAPI = (params) => {
  return request.get('/bookmarks', { params });
};

/**
 * 获取书签详情
 * @param {Number} id - 书签ID
 */
export const getBookmarkByIdAPI = (id) => {
  return request.get(`/bookmarks/${id}`);
};

/**
 * 创建书签
 * @param {Object} data - {title, url, description, categoryId, tags, isFavorite}
 */
export const createBookmarkAPI = (data) => {
  return request.post('/bookmarks', data);
};

/**
 * 更新书签
 * @param {Number} id - 书签ID
 * @param {Object} data - {title, description, categoryId, tags, isFavorite}
 */
export const updateBookmarkAPI = (id, data) => {
  return request.put(`/bookmarks/${id}`, data);
};

/**
 * 删除书签
 * @param {Number} id - 书签ID
 */
export const deleteBookmarkAPI = (id) => {
  return request.delete(`/bookmarks/${id}`);
};

/**
 * 批量删除书签
 * @param {Array} ids - 书签ID数组
 */
export const batchDeleteBookmarksAPI = (ids) => {
  return request.delete('/bookmarks/batch', { data: { ids } });
};

/**
 * 收藏/取消收藏书签
 * @param {Number} id - 书签ID
 * @param {Number} isFavorite - 0或1
 */
export const updateBookmarkFavoriteAPI = (id, isFavorite) => {
  return request.put(`/bookmarks/${id}/favorite`, { isFavorite });
};

/**
 * 记录书签访问
 * @param {Number} id - 书签ID
 */
export const recordBookmarkVisitAPI = (id) => {
  return request.put(`/bookmarks/${id}/visit`);
};

// ========== 回收站相关 API ==========

/**
 * 获取回收站书签
 */
export const getTrashBookmarksAPI = () => {
  return request.get('/bookmarks/trash');
};

/**
 * 恢复书签
 * @param {Number} id - 书签ID
 */
export const restoreBookmarkAPI = (id) => {
  return request.put(`/bookmarks/${id}/restore`);
};

/**
 * 永久删除书签
 * @param {Number} id - 书签ID
 */
export const permanentDeleteBookmarkAPI = (id) => {
  return request.delete(`/bookmarks/${id}/permanent`);
};

/**
 * 清空回收站
 */
export const clearTrashAPI = () => {
  return request.delete('/bookmarks/trash/clear');
};

// ========== 置顶书签相关 API ==========

/**
 * 置顶书签
 * @param {Number} id - 书签ID
 */
export const pinBookmarkAPI = (id) => {
  return request.put(`/bookmarks/${id}/pin`);
};

/**
 * 取消置顶
 * @param {Number} id - 书签ID
 */
export const unpinBookmarkAPI = (id) => {
  return request.put(`/bookmarks/${id}/unpin`);
};

/**
 * 获取置顶书签列表
 */
export const getPinnedBookmarksAPI = () => {
  return request.get('/bookmarks/pinned');
};

// ========== 高级搜索相关 API ==========

/**
 * 高级搜索
 * @param {Object} params - {keyword, domain, categoryId, startDate, endDate, linkStatus, page, size}
 */
export const advancedSearchAPI = (params) => {
  return request.get('/bookmarks/advanced-search', { params });
};

/**
 * 获取失效链接列表
 */
export const getDeadLinksAPI = () => {
  return request.get('/bookmarks/dead-links');
};