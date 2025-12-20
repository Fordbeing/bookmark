-- 修复激活码表缺少 update_time 字段的问题

-- 直接添加 update_time 字段（如果字段已存在会报错，这是正常的，可以忽略）
ALTER TABLE activation_code 
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
