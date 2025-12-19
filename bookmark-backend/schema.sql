-- 书签管理系统数据库脚本
-- MySQL 8.0+

-- 创建数据库
CREATE DATABASE IF NOT EXISTS bookmark_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookmark_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `email` VARCHAR(100) UNIQUE COMMENT '邮箱(微信登录可空)',
  `password` VARCHAR(255) COMMENT '密码(BCrypt加密,微信登录可空)',
  `phone` VARCHAR(20) UNIQUE COMMENT '手机号(用于账户绑定)',
  `openid` VARCHAR(100) UNIQUE COMMENT '微信小程序OpenID',
  `unionid` VARCHAR(100) COMMENT '微信UnionID(用于多平台)',
  `login_type` TINYINT DEFAULT 1 COMMENT '登录方式: 1-邮箱 2-微信 3-手机',
  `avatar` VARCHAR(500) COMMENT '头像URL',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` DATETIME COMMENT '最后登录时间',
  INDEX idx_email (`email`),
  INDEX idx_username (`username`),
  INDEX idx_openid (`openid`),
  INDEX idx_phone (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 书签表
CREATE TABLE IF NOT EXISTS `bookmark` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '书签ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NOT NULL COMMENT '书签标题',
  `url` VARCHAR(2048) NOT NULL COMMENT '书签URL',
  `description` TEXT COMMENT '描述',
  `icon_url` VARCHAR(500) COMMENT '网站图标URL',
  `category_id` BIGINT COMMENT '分类ID',
  `tags` VARCHAR(500) COMMENT '标签(JSON数组)',
  `is_favorite` TINYINT DEFAULT 0 COMMENT '是否收藏: 0-否 1-是',
  `visit_count` INT DEFAULT 0 COMMENT '访问次数',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-删除 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_category_id (`category_id`),
  INDEX idx_create_time (`create_time`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书签表';

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `color` VARCHAR(20) COMMENT '分类颜色',
  `icon` VARCHAR(50) COMMENT '分类图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  UNIQUE KEY uk_user_name (`user_id`, `name`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 用户设置表
CREATE TABLE IF NOT EXISTS `user_settings` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
  `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
  `theme` VARCHAR(20) DEFAULT 'light' COMMENT '主题: light/dark/auto',
  `primary_color` VARCHAR(20) DEFAULT '#2563eb' COMMENT '主色',
  `secondary_color` VARCHAR(20) DEFAULT '#1e40af' COMMENT '辅助色',
  `accent_color` VARCHAR(20) DEFAULT '#f59e0b' COMMENT '强调色',
  `background_color` VARCHAR(20) DEFAULT '#ffffff' COMMENT '背景色',
  `sidebar_color_from` VARCHAR(20) DEFAULT '#2563eb' COMMENT '侧边栏渐变起始色',
  `sidebar_color_to` VARCHAR(20) DEFAULT '#1e3a8a' COMMENT '侧边栏渐变结束色',
  `display_mode` VARCHAR(20) DEFAULT 'card' COMMENT '显示模式: card/list/compact',
  `auto_open_new_tab` TINYINT DEFAULT 1 COMMENT '自动新标签打开',
  `show_stats` TINYINT DEFAULT 1 COMMENT '显示统计信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

-- 标签表
-- 在 MySQL 中执行此脚本添加标签表

CREATE TABLE IF NOT EXISTS `tag` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `color` VARCHAR(20) DEFAULT '#6b7280' COMMENT '标签颜色',
  `usage_count` INT DEFAULT 0 COMMENT '使用次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  UNIQUE KEY uk_user_name (`user_id`, `name`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

CREATE TABLE user_activation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code_id BIGINT NOT NULL,
    extra_bookmarks INT DEFAULT 0,
    extra_categories INT DEFAULT 0,
    expire_time DATETIME NOT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_code (user_id, code_id)
);

CREATE TABLE activation_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) UNIQUE NOT NULL,
    extra_bookmarks INT DEFAULT 10,
    extra_categories INT DEFAULT 3,
    expire_days INT DEFAULT 30,
    max_uses INT DEFAULT 1,
    used_count INT DEFAULT 0,
    created_by BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user ADD COLUMN is_admin TINYINT DEFAULT 0;

-- 插入测试数据 (可选)
-- 密码: 123456 (BCrypt加密后)
INSERT INTO `user` (`username`, `email`, `password`) VALUES 
('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH');
