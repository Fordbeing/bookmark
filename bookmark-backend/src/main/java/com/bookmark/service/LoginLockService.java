package com.bookmark.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录锁定服务
 * 使用 Redis 实现登录失败计数和账户锁定
 */
@Slf4j
@Service
public class LoginLockService {

    private static final String LOCK_KEY_PREFIX = "login:lock:";
    private static final String FAIL_COUNT_PREFIX = "login:fail:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 检查账户是否被锁定
     * 
     * @param email 用户邮箱
     * @return 剩余锁定秒数，0表示未锁定
     */
    public long isLocked(String email) {
        try {
            String lockKey = LOCK_KEY_PREFIX + email;
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.error("检查锁定状态失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 记录登录失败
     * 
     * @param email 用户邮箱
     * @return 当前失败次数
     */
    public int recordLoginFailure(String email) {
        try {
            String failKey = FAIL_COUNT_PREFIX + email;
            Long count = redisTemplate.opsForValue().increment(failKey);

            // 设置失败计数的过期时间（与锁定时长一致）
            int lockMinutes = getLockDurationMinutes();
            redisTemplate.expire(failKey, lockMinutes, TimeUnit.MINUTES);

            int currentCount = count != null ? count.intValue() : 1;
            int maxAttempts = getMaxLoginAttempts();

            // 如果超过最大尝试次数，锁定账户
            if (currentCount >= maxAttempts) {
                lockAccount(email);
            }

            return currentCount;
        } catch (Exception e) {
            log.error("记录登录失败次数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 锁定账户
     */
    private void lockAccount(String email) {
        try {
            String lockKey = LOCK_KEY_PREFIX + email;
            int lockMinutes = getLockDurationMinutes();
            redisTemplate.opsForValue().set(lockKey, "locked", lockMinutes, TimeUnit.MINUTES);

            // 清除失败计数
            redisTemplate.delete(FAIL_COUNT_PREFIX + email);

            log.info("账户已被锁定: email={}, 锁定时长={}分钟", email, lockMinutes);
        } catch (Exception e) {
            log.error("锁定账户失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 登录成功，清除失败计数和锁定
     */
    public void clearLoginFailure(String email) {
        try {
            redisTemplate.delete(FAIL_COUNT_PREFIX + email);
            redisTemplate.delete(LOCK_KEY_PREFIX + email);
        } catch (Exception e) {
            log.error("清除登录失败记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取当前失败次数
     */
    public int getFailCount(String email) {
        try {
            String failKey = FAIL_COUNT_PREFIX + email;
            String count = redisTemplate.opsForValue().get(failKey);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            log.error("获取失败次数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取剩余尝试次数
     */
    public int getRemainingAttempts(String email) {
        int maxAttempts = getMaxLoginAttempts();
        int failCount = getFailCount(email);
        return Math.max(0, maxAttempts - failCount);
    }

    /**
     * 获取最大登录尝试次数（从系统配置读取）
     */
    public int getMaxLoginAttempts() {
        String config = systemConfigService.getConfig("login_fail_lock_count");
        if (config != null) {
            try {
                return Integer.parseInt(config);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 5; // 默认5次
    }

    /**
     * 获取锁定时长（分钟，从系统配置读取）
     */
    public int getLockDurationMinutes() {
        String config = systemConfigService.getConfig("lock_duration_minutes");
        if (config != null) {
            try {
                return Integer.parseInt(config);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 30; // 默认30分钟
    }
}
