-- 批量书签分享表
CREATE TABLE IF NOT EXISTS `shared_bookmarks` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '分享者用户ID',
    `title` VARCHAR(255) DEFAULT '分享的书签' COMMENT '分享标题',
    `bookmark_ids` TEXT NOT NULL COMMENT '书签ID列表，逗号分隔',
    `share_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '分享码',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '访问密码(加密)',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
    `view_count` INT DEFAULT 0 COMMENT '访问次数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-有效 0-已取消',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_share_code` (`share_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量书签分享表';
