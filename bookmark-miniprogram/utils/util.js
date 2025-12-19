// utils/util.js
// 工具函数

/**
 * 格式化日期
 * @param {string|Date} date - 日期
 * @returns {string} 格式化后的日期字符串
 */
const formatDate = (date) => {
    if (!date) return '';

    const d = new Date(date);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
        const hours = Math.floor(diff / (1000 * 60 * 60));
        if (hours === 0) {
            const minutes = Math.floor(diff / (1000 * 60));
            if (minutes <= 0) return '刚刚';
            return `${minutes}分钟前`;
        }
        return `${hours}小时前`;
    }
    if (days === 1) return '昨天';
    if (days < 7) return `${days}天前`;

    const month = d.getMonth() + 1;
    const day = d.getDate();
    return `${month}-${day}`;
};

/**
 * 解析标签
 * @param {string|Array} tags - 标签（可能是 JSON 字符串或数组）
 * @returns {Array} 标签数组
 */
const parseTags = (tags) => {
    if (!tags) return [];
    if (Array.isArray(tags)) return tags;
    try {
        return JSON.parse(tags);
    } catch (e) {
        return [];
    }
};

/**
 * 获取域名
 * @param {string} url - URL
 * @returns {string} 域名
 */
const getDomain = (url) => {
    if (!url) return '';
    try {
        const urlObj = new URL(url);
        return urlObj.hostname;
    } catch (e) {
        // 简单解析
        const match = url.match(/^https?:\/\/([^\/]+)/);
        return match ? match[1] : url;
    }
};

/**
 * 复制到剪贴板
 * @param {string} text - 要复制的文本
 */
const copyToClipboard = (text) => {
    return new Promise((resolve, reject) => {
        wx.setClipboardData({
            data: text,
            success: () => {
                wx.showToast({
                    title: '已复制到剪贴板',
                    icon: 'success'
                });
                resolve();
            },
            fail: (err) => {
                wx.showToast({
                    title: '复制失败',
                    icon: 'none'
                });
                reject(err);
            }
        });
    });
};

/**
 * 显示确认弹窗
 * @param {string} title - 标题
 * @param {string} content - 内容
 * @returns {Promise<boolean>}
 */
const showConfirm = (title, content) => {
    return new Promise((resolve) => {
        wx.showModal({
            title: title,
            content: content,
            confirmText: '确定',
            cancelText: '取消',
            success: (res) => {
                resolve(res.confirm);
            },
            fail: () => {
                resolve(false);
            }
        });
    });
};

/**
 * 显示操作菜单
 * @param {Array<string>} itemList - 菜单项
 * @returns {Promise<number>} 选中的索引，取消返回 -1
 */
const showActionSheet = (itemList) => {
    return new Promise((resolve) => {
        wx.showActionSheet({
            itemList: itemList,
            success: (res) => {
                resolve(res.tapIndex);
            },
            fail: () => {
                resolve(-1);
            }
        });
    });
};

module.exports = {
    formatDate,
    parseTags,
    getDomain,
    copyToClipboard,
    showConfirm,
    showActionSheet
};
