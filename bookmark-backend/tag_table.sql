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
