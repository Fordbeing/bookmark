package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmark.entity.SystemConfig;
import com.bookmark.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置服务
 */
@Slf4j
@Service
public class SystemConfigService {

    private static final String REDIS_PREFIX = "system:config:";

    @Autowired
    private SystemConfigMapper configMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取所有配置
     */
    public Map<String, String> getAllConfig() {
        Map<String, String> config = new HashMap<>();
        try {
            List<SystemConfig> list = configMapper.selectList(null);
            for (SystemConfig sc : list) {
                config.put(sc.getConfigKey(), sc.getConfigValue());
            }
        } catch (Exception e) {
            log.error("获取所有配置失败: {}", e.getMessage(), e);
        }
        return config;
    }

    /**
     * 获取配置值（优先从 Redis 获取）
     */
    public String getConfig(String key) {
        try {
            // 先从 Redis 获取
            String value = redisTemplate.opsForValue().get(REDIS_PREFIX + key);
            if (value != null) {
                return value;
            }
        } catch (Exception e) {
            log.error("从 Redis 获取配置失败: {}", e.getMessage(), e);
        }

        try {
            // 从数据库获取
            LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemConfig::getConfigKey, key);
            SystemConfig config = configMapper.selectOne(wrapper);

            if (config != null) {
                // 缓存到 Redis
                try {
                    redisTemplate.opsForValue().set(REDIS_PREFIX + key, config.getConfigValue(), 1, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.error("缓存配置到 Redis 失败: {}", e.getMessage(), e);
                }
                return config.getConfigValue();
            }
        } catch (Exception e) {
            log.error("从数据库获取配置失败: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 获取配置值（返回布尔值）
     */
    public boolean getConfigBoolean(String key, boolean defaultValue) {
        String value = getConfig(key);
        if (value == null)
            return defaultValue;
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    /**
     * 更新配置
     */
    public void setConfig(String key, String value) {
        try {
            LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemConfig::getConfigKey, key);
            SystemConfig config = configMapper.selectOne(wrapper);

            if (config != null) {
                config.setConfigValue(value);
                configMapper.updateById(config);
            } else {
                config = new SystemConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                configMapper.insert(config);
            }
        } catch (Exception e) {
            log.error("更新数据库配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新配置失败");
        }

        // 更新 Redis 缓存
        try {
            redisTemplate.opsForValue().set(REDIS_PREFIX + key, value, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("更新 Redis 缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 批量更新配置
     */
    public void updateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            setConfig(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 检查是否允许登录
     */
    public boolean isLoginAllowed() {
        return getConfigBoolean("allow_login", true);
    }

    /**
     * 检查是否处于维护模式
     */
    public boolean isMaintenanceMode() {
        return getConfigBoolean("maintenance_mode", false);
    }

    /**
     * 获取维护提示信息
     */
    public String getMaintenanceMessage() {
        String msg = getConfig("maintenance_message");
        return msg != null ? msg : "系统维护中，请稍后再试";
    }
}
