import request from './request';

/**
 * 获取分类列表
 */
export const getCategoryListAPI = () => {
    return request.get('/categories');
};

/**
 * 创建分类
 * @param {Object} data - {name, color, icon}
 */
export const createCategoryAPI = (data) => {
    return request.post('/categories', data);
};

/**
 * 更新分类
 * @param {Number} id - 分类ID
 * @param {Object} data - {name, color, icon}
 */
export const updateCategoryAPI = (id, data) => {
    return request.put(`/categories/${id}`, data);
};

/**
 * 删除分类
 * @param {Number} id - 分类ID
 */
export const deleteCategoryAPI = (id) => {
    return request.delete(`/categories/${id}`);
};
