package com.bookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 管理服务
 * 使用 Redis 存储和验证 Token，支持用户禁用时立即注销
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    // Redis Key 前缀
    private static final String ACCESS_TOKEN_PREFIX = "token:access:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final String USER_TOKENS_PREFIX = "token:user:";

    // Token 有效期
    private static final long ACCESS_TOKEN_EXPIRE_HOURS = 2;
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 7;

    // 刷新阈值：剩余时间少于30分钟时建议刷新
    private static final long REFRESH_THRESHOLD_MINUTES = 30;

    /**
     * 生成 Token ID
     */
    public String generateTokenId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 存储 Access Token
     */
    public void storeAccessToken(Long userId, String tokenId) {
        try {
            String key = ACCESS_TOKEN_PREFIX + userId + ":" + tokenId;
            redisTemplate.opsForValue().set(key, "1", ACCESS_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);

            // 添加到用户 Token 索引
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, "access:" + tokenId);
            redisTemplate.expire(userTokensKey, REFRESH_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

            log.debug("存储 Access Token: userId={}, tokenId={}", userId, tokenId);
        } catch (Exception e) {
            log.error("存储 Access Token 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 存储 Refresh Token
     */
    public void storeRefreshToken(Long userId, String tokenId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
            redisTemplate.opsForValue().set(key, "1", REFRESH_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

            // 添加到用户 Token 索引
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, "refresh:" + tokenId);
            redisTemplate.expire(userTokensKey, REFRESH_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

            log.debug("存储 Refresh Token: userId={}, tokenId={}", userId, tokenId);
        } catch (Exception e) {
            log.error("存储 Refresh Token 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 验证 Access Token 是否有效
     */
    public boolean validateAccessToken(Long userId, String tokenId) {
        try {
            String key = ACCESS_TOKEN_PREFIX + userId + ":" + tokenId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("验证 Access Token 失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证 Refresh Token 是否有效
     */
    public boolean validateRefreshToken(Long userId, String tokenId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("验证 Refresh Token 失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查 Access Token 是否需要刷新
     * 当剩余有效期小于阈值时返回 true
     */
    public boolean shouldRefreshAccessToken(Long userId, String tokenId) {
        try {
            String key = ACCESS_TOKEN_PREFIX + userId + ":" + tokenId;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            return ttl != null && ttl > 0 && ttl < REFRESH_THRESHOLD_MINUTES;
        } catch (Exception e) {
            log.error("检查 Token 刷新状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 刷新 Access Token（延长有效期）
     */
    public void refreshAccessToken(Long userId, String tokenId) {
        try {
            String key = ACCESS_TOKEN_PREFIX + userId + ":" + tokenId;
            redisTemplate.expire(key, ACCESS_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("刷新 Access Token: userId={}, tokenId={}", userId, tokenId);
        } catch (Exception e) {
            log.error("刷新 Access Token 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 删除指定 Access Token
     */
    public void revokeAccessToken(Long userId, String tokenId) {
        try {
            String key = ACCESS_TOKEN_PREFIX + userId + ":" + tokenId;
            redisTemplate.delete(key);

            // 从用户索引中移除
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokensKey, "access:" + tokenId);

            log.debug("删除 Access Token: userId={}, tokenId={}", userId, tokenId);
        } catch (Exception e) {
            log.error("删除 Access Token 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 删除指定 Refresh Token
     */
    public void revokeRefreshToken(Long userId, String tokenId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
            redisTemplate.delete(key);

            // 从用户索引中移除
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokensKey, "refresh:" + tokenId);

            log.debug("删除 Refresh Token: userId={}, tokenId={}", userId, tokenId);
        } catch (Exception e) {
            log.error("删除 Refresh Token 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 注销用户所有 Token（用户禁用时调用）
     */
    public void revokeAllUserTokens(Long userId) {
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

            if (tokens != null && !tokens.isEmpty()) {
                for (String tokenInfo : tokens) {
                    String[] parts = tokenInfo.split(":");
                    if (parts.length == 2) {
                        String type = parts[0];
                        String tokenId = parts[1];
                        String key = ("access".equals(type) ? ACCESS_TOKEN_PREFIX : REFRESH_TOKEN_PREFIX)
                                + userId + ":" + tokenId;
                        redisTemplate.delete(key);
                    }
                }
            }

            // 删除用户 Token 索引
            redisTemplate.delete(userTokensKey);

            log.info("已注销用户所有 Token: userId={}", userId);
        } catch (Exception e) {
            log.error("注销用户所有 Token 失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 获取 Access Token 有效期（小时）
     */
    public long getAccessTokenExpireHours() {
        return ACCESS_TOKEN_EXPIRE_HOURS;
    }

    /**
     * 获取 Refresh Token 有效期（天）
     */
    public long getRefreshTokenExpireDays() {
        return REFRESH_TOKEN_EXPIRE_DAYS;
    }

    /**
     * 获取当前在线用户数
     * 通过统计 Redis 中拥有有效 Access Token 的用户数量来判断
     */
    public long getOnlineUserCount() {
        try {
            // 扫描所有活跃的 Access Token 键
            Set<String> accessTokenKeys = redisTemplate.keys(ACCESS_TOKEN_PREFIX + "*");
            if (accessTokenKeys == null || accessTokenKeys.isEmpty()) {
                return 0;
            }

            // 提取唯一的用户 ID
            Set<Long> onlineUserIds = new java.util.HashSet<>();
            for (String key : accessTokenKeys) {
                // key 格式: token:access:{userId}:{tokenId}
                String[] parts = key.replace(ACCESS_TOKEN_PREFIX, "").split(":");
                if (parts.length >= 1) {
                    try {
                        Long userId = Long.parseLong(parts[0]);
                        onlineUserIds.add(userId);
                    } catch (NumberFormatException e) {
                        // 忽略解析错误的 key
                    }
                }
            }

            log.debug("当前在线用户数: {}", onlineUserIds.size());
            return onlineUserIds.size();
        } catch (Exception e) {
            log.error("获取在线用户数失败: {}", e.getMessage(), e);
            return 0;
        }
    }
}
