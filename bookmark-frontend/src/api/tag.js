import request from './request';

/**
 * 获取标签列表
 */
export const getTagListAPI = () => {
    return request.get('/tags');
};

/**
 * 创建标签
 * @param {Object} data - {name, color}
 */
export const createTagAPI = (data) => {
    return request.post('/tags', data);
};

/**
 * 更新标签
 * @param {Number} id - 标签ID
 * @param {Object} data - {name, color}
 */
export const updateTagAPI = (id, data) => {
    return request.put(`/tags/${id}`, data);
};

/**
 * 删除标签
 * @param {Number} id - 标签ID
 */
export const deleteTagAPI = (id) => {
    return request.delete(`/tags/${id}`);
};

/**
 * 增加标签使用次数
 * @param {Number} id - 标签ID
 */
export const incrementTagUsageAPI = (id) => {
    return request.post(`/tags/${id}/increment`);
};
