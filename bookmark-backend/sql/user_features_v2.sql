-- 用户端新功能数据库变更
-- 执行时间: 2025-12-21
-- 功能: 置顶书签、链接健康检查、分享功能
-- 注意: 如果字段已存在会报错，可忽略对应的ALTER语句错误

-- ============================================
-- 1. 书签表新增字段（置顶、链接健康检查）
-- 如果提示 Duplicate column name 错误，说明字段已存在，可忽略
-- ============================================
ALTER TABLE bookmark ADD COLUMN is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶: 0-否 1-是';
ALTER TABLE bookmark ADD COLUMN link_status TINYINT DEFAULT 0 COMMENT '链接状态: 0-未检测 1-正常 2-失效 3-重定向 4-超时';
ALTER TABLE bookmark ADD COLUMN last_check_time DATETIME COMMENT '最后检测时间';
ALTER TABLE bookmark ADD COLUMN check_message VARCHAR(255) COMMENT '检测结果描述';

-- ============================================
-- 2. 分享链接表
-- ============================================
CREATE TABLE IF NOT EXISTS shared_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    share_code VARCHAR(32) NOT NULL UNIQUE COMMENT '分享码',
    password VARCHAR(100) COMMENT '访问密码(BCrypt加密)',
    expire_time DATETIME COMMENT '过期时间',
    view_count INT DEFAULT 0 COMMENT '访问次数',
    status TINYINT DEFAULT 1 COMMENT '1-有效 0-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_share_code (share_code),
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享链接表';

-- ============================================
-- 3. 添加索引优化查询（忽略重复索引错误）
-- ============================================
ALTER TABLE bookmark ADD INDEX idx_bookmark_is_pinned (is_pinned);
ALTER TABLE bookmark ADD INDEX idx_bookmark_link_status (link_status);
ALTER TABLE bookmark ADD INDEX idx_bookmark_last_check_time (last_check_time);
