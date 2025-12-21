-- 管理员后台相关数据库表
-- 执行此脚本前请确保已选择正确的数据库: USE bookmark_db;

-- 操作日志表
CREATE TABLE IF NOT EXISTS `admin_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `admin_id` BIGINT NOT NULL COMMENT '操作管理员ID',
  `action_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(50) COMMENT '目标类型(user/bookmark/activation_code)',
  `target_id` BIGINT COMMENT '目标ID',
  `old_value` JSON COMMENT '修改前的值',
  `new_value` JSON COMMENT '修改后的值',
  `ip_address` VARCHAR(50) COMMENT 'IP地址',
  `user_agent` VARCHAR(500) COMMENT '浏览器信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_admin_time (`admin_id`, `create_time`),
  INDEX idx_action_type (`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `config_key` VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `config_type` VARCHAR(20) DEFAULT 'string' COMMENT '值类型: string/number/boolean/json',
  `description` VARCHAR(255) COMMENT '配置描述',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('default_bookmark_limit', '100', '新用户默认书签数量限制'),
('default_category_limit', '10', '新用户默认分类数量限制'),
('allow_registration', '1', '是否允许新用户注册'),
('enable_wechat_login', '1', '是否启用微信登录'),
('allow_login', '1', '是否允许用户登录'),
('maintenance_mode', '0', '系统维护模式'),
('maintenance_message', '', '维护模式提示信息')
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;

-- 公告表
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `type` ENUM('info','warning','maintenance','update') DEFAULT 'info' COMMENT '公告类型',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `target` VARCHAR(50) DEFAULT 'all' COMMENT '目标用户群',
  `start_time` DATETIME COMMENT '开始显示时间',
  `end_time` DATETIME COMMENT '结束显示时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `view_count` INT DEFAULT 0 COMMENT '查看次数',
  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status_time (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 登录历史表
CREATE TABLE IF NOT EXISTS `login_history` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `login_type` TINYINT COMMENT '登录方式: 1-邮箱 2-微信 3-手机',
  `ip_address` VARCHAR(50) COMMENT 'IP地址',
  `location` VARCHAR(100) COMMENT '登录地点',
  `device` VARCHAR(100) COMMENT '设备信息',
  `browser` VARCHAR(100) COMMENT '浏览器信息',
  `status` TINYINT COMMENT '登录状态: 0-失败 1-成功',
  `fail_reason` VARCHAR(100) COMMENT '失败原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_time (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录历史表';

-- 用户反馈表
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` ENUM('bug','feature','complaint','other') DEFAULT 'other' COMMENT '反馈类型',
  `title` VARCHAR(255) NOT NULL COMMENT '反馈标题',
  `content` TEXT NOT NULL COMMENT '反馈内容',
  `contact` VARCHAR(100) COMMENT '联系方式',
  `screenshots` JSON COMMENT '截图URL列表',
  `status` ENUM('pending','processing','resolved','closed') DEFAULT 'pending' COMMENT '处理状态',
  `handler_id` BIGINT COMMENT '处理人ID',
  `reply` TEXT COMMENT '回复内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user (`user_id`),
  INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- 站内通知表
CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `type` VARCHAR(20) DEFAULT 'system' COMMENT '通知类型',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_read (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

ALTER TABLE admin_log 
MODIFY COLUMN old_value TEXT,
MODIFY COLUMN new_value TEXT;

ALTER TABLE `user_settings` 
ADD COLUMN `default_bookmark_limit` int DEFAULT 50 COMMENT '默认书签上限' AFTER `show_stats`,
ADD COLUMN `default_category_limit` int DEFAULT 10 COMMENT '默认分类上限' AFTER `default_bookmark_limit`;