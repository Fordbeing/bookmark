package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bookmark.entity.UserActivation;
import com.bookmark.mapper.UserActivationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 激活码过期处理定时任务
 * 使用 Redis ZSet 存储过期时间，定时扫描并更新过期激活码状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationExpiryTask {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserActivationMapper userActivationMapper;

    /**
     * Redis ZSet Key，用于存储激活码过期时间
     * Score: 过期时间的时间戳（秒）
     * Value: 激活码ID
     */
    private static final String ACTIVATION_EXPIRY_KEY = "activation:expiry:zset";

    /**
     * 添加激活码到过期队列
     * 
     * @param activationId 激活码ID
     * @param expireTime   过期时间
     */
    public void addToExpiryQueue(Long activationId, LocalDateTime expireTime) {
        try {
            // 将 LocalDateTime 转换为时间戳（秒）
            long score = expireTime.atZone(ZoneId.systemDefault()).toEpochSecond();

            // 添加到 ZSet，score 是过期时间戳
            redisTemplate.opsForZSet().add(ACTIVATION_EXPIRY_KEY, activationId.toString(), score);

            log.debug("激活码 {} 已加入过期队列，过期时间: {}", activationId, expireTime);
        } catch (Exception e) {
            log.error("添加激活码到过期队列失败: activationId={}, expireTime={}",
                    activationId, expireTime, e);
        }
    }

    /**
     * 定时任务：每10秒扫描一次，处理已过期的激活码
     * fixedRate = 10000ms = 10秒
     */
    @Scheduled(fixedRate = 10000)
    public void processExpiredActivations() {
        try {
            long now = System.currentTimeMillis() / 1000; // 当前时间戳（秒）

            // 从 ZSet 中获取所有分数（过期时间）<= 当前时间的激活码
            Set<String> expiredIdStrings = redisTemplate.opsForZSet()
                    .rangeByScore(ACTIVATION_EXPIRY_KEY, 0, now);

            if (expiredIdStrings == null || expiredIdStrings.isEmpty()) {
                return; // 没有过期的激活码，直接返回
            }

            // 转换为 Long 类型的 ID 列表
            List<Long> expiredIds = expiredIdStrings.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            log.info("发现 {} 个过期激活码，准备更新状态", expiredIds.size());

            // 批量更新激活码状态为 0（已过期）
            int updatedCount = userActivationMapper.update(null,
                    new UpdateWrapper<UserActivation>()
                            .in("id", expiredIds)
                            .eq("status", 1) // 只更新状态为1的
                            .set("status", 0));

            // 从 ZSet 中移除已处理的激活码
            Long removedCount = redisTemplate.opsForZSet()
                    .remove(ACTIVATION_EXPIRY_KEY, expiredIdStrings.toArray());

            log.info("成功处理过期激活码: 更新数据库 {} 条，从Redis移除 {} 条",
                    updatedCount, removedCount);

        } catch (Exception e) {
            log.error("处理过期激活码失败", e);
        }
    }

    /**
     * 清理 Redis ZSet 中的无效数据
     * 每天凌晨3点执行一次
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiryQueue() {
        try {
            // 获取当前 ZSet 大小
            Long size = redisTemplate.opsForZSet().size(ACTIVATION_EXPIRY_KEY);
            log.info("开始清理过期队列，当前大小: {}", size);

            // 移除7天前的所有记录（防止数据堆积）
            long sevenDaysAgo = System.currentTimeMillis() / 1000 - (7 * 24 * 60 * 60);
            Long removed = redisTemplate.opsForZSet()
                    .removeRangeByScore(ACTIVATION_EXPIRY_KEY, 0, sevenDaysAgo);

            log.info("清理完成，移除了 {} 条过期记录", removed);
        } catch (Exception e) {
            log.error("清理过期队列失败", e);
        }
    }

    /**
     * 系统启动时，同步现有的未过期激活码到 Redis
     * 防止redis数据丢失后无法恢复
     */
    public void syncActiveActivationsToRedis() {
        try {
            log.info("开始同步激活码到Redis过期队列...");

            // 查询所有未过期且状态为1的激活码
            List<UserActivation> activeActivations = userActivationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserActivation>()
                            .eq("status", 1)
                            .gt("expire_time", LocalDateTime.now()));

            if (activeActivations.isEmpty()) {
                log.info("没有需要同步的激活码");
                return;
            }

            // 批量添加到 Redis ZSet
            for (UserActivation activation : activeActivations) {
                addToExpiryQueue(activation.getId(), activation.getExpireTime());
            }

            log.info("同步完成，共同步 {} 个激活码到Redis", activeActivations.size());
        } catch (Exception e) {
            log.error("同步激活码到Redis失败", e);
        }
    }
}
