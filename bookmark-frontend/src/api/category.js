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
 * @param {Boolean} deleteBookmarks - 是否删除分类下的书签（false则移至未分类）
 */
export const deleteCategoryAPI = (id, deleteBookmarks = false) => {
    return request.delete(`/categories/${id}?deleteBookmarks=${deleteBookmarks}`);
};

/**
 * 更新分类排序
 * @param {Array<Number>} categoryIds - 分类ID列表，按新顺序排列
 */
export const updateCategorySortAPI = (categoryIds) => {
    return request.put('/categories/sort', categoryIds);
};
