import request from './request';

/**
 * 搜索书签
 * @param {string} keyword - 搜索关键词
 */
export const searchBookmarksAPI = (keyword) => {
    return request.get('/search', { params: { keyword } });
};
