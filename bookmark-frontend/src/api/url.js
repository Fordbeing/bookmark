import request from './request';

/**
 * 获取 URL 元数据（标题、描述、图标）
 * @param {String} url - 要获取元数据的 URL
 * @returns {Promise} 包含 title, description, iconUrl 的对象
 */
export const fetchUrlMetadataAPI = (url) => {
    return request.get('/url/metadata', { params: { url } });
};
