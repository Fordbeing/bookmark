import request from './request';

/**
 * 导出书签数据
 * @returns {Promise} 导出响应，包含书签、分类、标签数据
 */
export const exportBookmarksAPI = () => {
    return request.get('/data/export');
};

/**
 * 导入书签（JSON body 方式）
 * @param {Object} data - { importType: 'CHROME'|'EDGE'|'JSON', content: string }
 */
export const importBookmarksAPI = (data) => {
    return request.post('/data/import', data);
};

/**
 * 导入书签（文件上传方式）
 * @param {File} file - 要上传的文件
 * @param {String} type - 导入类型: CHROME, EDGE, JSON
 */
export const importBookmarksFileAPI = (file, type) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    return request.post('/data/import/file', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
};

/**
 * 清除所有用户数据（需要密码验证）
 * @param {String} password - 用户密码
 */
export const clearAllDataAPI = (password) => {
    return request.delete('/data/clear', { data: { password } });
};

/**
 * 下载导出的数据为 JSON 文件
 */
export const downloadExportData = async () => {
    const response = await exportBookmarksAPI();
    if (response.data) {
        const json = JSON.stringify(response.data, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `bookmarks-backup-${new Date().toISOString().slice(0, 10)}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        return true;
    }
    return false;
};
