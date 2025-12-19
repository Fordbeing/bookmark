-- 激活码系统数据库脚本

-- 1. 修改 user 表添加管理员标识
ALTER TABLE user ADD COLUMN is_admin TINYINT DEFAULT 0 COMMENT '0-普通用户 1-管理员';

-- 2. 创建激活码表
CREATE TABLE activation_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) UNIQUE NOT NULL COMMENT '激活码',
    extra_bookmarks INT DEFAULT 10 COMMENT '增加的书签数量',
    extra_categories INT DEFAULT 3 COMMENT '增加的分类数量',
    expire_days INT DEFAULT 30 COMMENT '有效天数',
    max_uses INT DEFAULT 1 COMMENT '最大使用次数',
    used_count INT DEFAULT 0 COMMENT '已使用次数',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. 创建用户激活记录表
CREATE TABLE user_activation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    code_id BIGINT NOT NULL COMMENT '激活码ID',
    extra_bookmarks INT DEFAULT 0 COMMENT '增加的书签数量',
    extra_categories INT DEFAULT 0 COMMENT '增加的分类数量',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    status TINYINT DEFAULT 1 COMMENT '0-已过期 1-生效中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_code (user_id, code_id)
);

-- 4. 设置管理员（请修改为你的用户ID）
-- UPDATE user SET is_admin = 1 WHERE id = 1;
