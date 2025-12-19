// api/category.js
// 分类相关 API

const { get, post, put, del } = require('../utils/request');

/**
 * 获取分类列表
 */
const getCategoryListAPI = () => {
    return get('/categories');
};

/**
 * 创建分类
 * @param {Object} data - {name, color, icon}
 */
const createCategoryAPI = (data) => {
    return post('/categories', data);
};

/**
 * 更新分类
 * @param {Number} id - 分类ID
 * @param {Object} data - {name, color, icon}
 */
const updateCategoryAPI = (id, data) => {
    return put(`/categories/${id}`, data);
};

/**
 * 删除分类
 * @param {Number} id - 分类ID
 */
const deleteCategoryAPI = (id) => {
    return del(`/categories/${id}`);
};

module.exports = {
    getCategoryListAPI,
    createCategoryAPI,
    updateCategoryAPI,
    deleteCategoryAPI
};
