import request from './request';

/**
 * 创建激活码（仅管理员）
 */
export const createActivationCodeAPI = (data) => {
    return request.post('/activation-codes', data);
};

/**
 * 获取激活码列表（仅管理员）
 */
export const getActivationCodeListAPI = () => {
    return request.get('/activation-codes');
};

/**
 * 兑换激活码
 */
export const redeemActivationCodeAPI = (code) => {
    return request.post('/activation-codes/redeem', { code });
};

/**
 * 获取当前用户的限制信息
 */
export const getMyLimitsAPI = () => {
    return request.get('/activation-codes/my-limits');
};
