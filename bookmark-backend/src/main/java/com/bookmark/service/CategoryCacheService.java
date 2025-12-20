package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.Category;
import com.bookmark.mapper.CategoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 分类缓存服务
 * 使用 Redis 缓存用户的分类列表，提高查询效率
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    /**
     * Redis Key 前缀
     */
    private static final String CATEGORY_CACHE_PREFIX = "user:categories:";

    /**
     * 缓存过期时间：30分钟
     */
    private static final long CACHE_EXPIRE_MINUTES = 30;

    /**
     * 获取用户分类列表（带缓存）
     * Cache-Aside 模式：先查 Redis，miss 再查 MySQL 并回写
     * 
     * @param userId 用户ID
     * @return 分类列表
     */
    public List<Category> getUserCategories(Long userId) {
        String cacheKey = CATEGORY_CACHE_PREFIX + userId;

        try {
            // 1. 尝试从 Redis 获取
            String cachedData = redisTemplate.opsForValue().get(cacheKey);

            if (cachedData != null) {
                // 缓存命中
                log.debug("从Redis获取用户{}的分类缓存", userId);
                return objectMapper.readValue(cachedData, new TypeReference<List<Category>>() {
                });
            }

            // 2. 缓存未命中，从 MySQL 查询
            log.debug("Redis缓存未命中，从MySQL查询用户{}的分类", userId);
            List<Category> categories = categoryMapper.selectList(
                    new QueryWrapper<Category>()
                            .eq("user_id", userId)
                            .orderByAsc("sort_order")
                            .orderByDesc("create_time"));

            // 3. 写入 Redis 缓存
            if (categories != null && !categories.isEmpty()) {
                String jsonData = objectMapper.writeValueAsString(categories);
                redisTemplate.opsForValue().set(cacheKey, jsonData, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                log.debug("已将用户{}的分类缓存到Redis，过期时间{}分钟", userId, CACHE_EXPIRE_MINUTES);
            }

            return categories;

        } catch (Exception e) {
            log.error("获取用户分类缓存失败，降级到MySQL查询: userId={}", userId, e);
            // 降级：直接查询 MySQL
            return categoryMapper.selectList(
                    new QueryWrapper<Category>()
                            .eq("user_id", userId)
                            .orderByAsc("sort_order")
                            .orderByDesc("create_time"));
        }
    }

    /**
     * 更新用户分类缓存
     * 在增加、修改、删除分类时调用
     * 
     * @param userId 用户ID
     */
    public void refreshUserCategoriesCache(Long userId) {
        String cacheKey = CATEGORY_CACHE_PREFIX + userId;

        try {
            // 从数据库查询最新数据
            List<Category> categories = categoryMapper.selectList(
                    new QueryWrapper<Category>()
                            .eq("user_id", userId)
                            .orderByAsc("sort_order")
                            .orderByDesc("create_time"));

            // 更新到 Redis
            if (categories != null && !categories.isEmpty()) {
                String jsonData = objectMapper.writeValueAsString(categories);
                redisTemplate.opsForValue().set(cacheKey, jsonData, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                log.debug("已刷新用户{}的分类缓存", userId);
            } else {
                // 如果分类为空，删除缓存
                redisTemplate.delete(cacheKey);
                log.debug("用户{}无分类数据，已删除缓存", userId);
            }

        } catch (Exception e) {
            log.error("刷新用户分类缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 删除用户分类缓存
     * 在删除分类或需要强制刷新时调用
     * 
     * @param userId 用户ID
     */
    public void invalidateUserCategoriesCache(Long userId) {
        String cacheKey = CATEGORY_CACHE_PREFIX + userId;

        try {
            redisTemplate.delete(cacheKey);
            log.debug("已删除用户{}的分类缓存", userId);
        } catch (Exception e) {
            log.error("删除用户分类缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 定时同步任务：每5分钟同步一次 Redis 和 MySQL
     * 确保缓存数据的一致性
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5分钟
    public void syncCategoryCacheWithDatabase() {
        try {
            log.info("开始同步分类缓存与数据库...");

            // 获取所有缓存的 key
            Set<String> cacheKeys = redisTemplate.keys(CATEGORY_CACHE_PREFIX + "*");

            if (cacheKeys == null || cacheKeys.isEmpty()) {
                log.info("没有需要同步的分类缓存");
                return;
            }

            int syncCount = 0;
            for (String cacheKey : cacheKeys) {
                try {
                    // 提取 userId
                    String userIdStr = cacheKey.substring(CATEGORY_CACHE_PREFIX.length());
                    Long userId = Long.parseLong(userIdStr);

                    // 从数据库查询最新数据
                    List<Category> categories = categoryMapper.selectList(
                            new QueryWrapper<Category>()
                                    .eq("user_id", userId)
                                    .orderByAsc("sort_order")
                                    .orderByDesc("create_time"));

                    // 更新缓存
                    if (categories != null && !categories.isEmpty()) {
                        String jsonData = objectMapper.writeValueAsString(categories);
                        redisTemplate.opsForValue().set(cacheKey, jsonData, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                        syncCount++;
                    } else {
                        // 数据库中没有数据，删除缓存
                        redisTemplate.delete(cacheKey);
                    }

                } catch (Exception e) {
                    log.error("同步缓存失败: cacheKey={}", cacheKey, e);
                }
            }

            log.info("分类缓存同步完成，共同步 {} 个用户", syncCount);

        } catch (Exception e) {
            log.error("分类缓存同步任务失败", e);
        }
    }

    /**
     * 清理过期缓存（可选）
     * Redis 会自动清理过期 key，此方法用于额外的清理逻辑
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void cleanExpiredCache() {
        try {
            log.info("开始清理过期的分类缓存...");

            Set<String> cacheKeys = redisTemplate.keys(CATEGORY_CACHE_PREFIX + "*");

            if (cacheKeys == null || cacheKeys.isEmpty()) {
                log.info("没有需要清理的分类缓存");
                return;
            }

            int cleanedCount = 0;
            for (String cacheKey : cacheKeys) {
                Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
                if (ttl != null && ttl < 0) {
                    // TTL 为 -2 表示key不存在，-1表示永不过期
                    redisTemplate.delete(cacheKey);
                    cleanedCount++;
                }
            }

            log.info("分类缓存清理完成，共清理 {} 个过期键", cleanedCount);

        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }
}
