package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.entity.Announcement;
import com.bookmark.mapper.AnnouncementMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 公告服务
 */
@Slf4j
@Service
public class AnnouncementService {

    private static final String REDIS_CHANNEL = "bookmark:announcement";
    private static final String REDIS_LATEST_KEY = "announcement:latest";

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 创建公告
     */
    public Announcement create(Announcement announcement) {
        try {
            announcement.setCreateTime(LocalDateTime.now());
            if (announcement.getStatus() == null) {
                announcement.setStatus(0); // 默认草稿
            }
            announcementMapper.insert(announcement);
            return announcement;
        } catch (Exception e) {
            log.error("创建公告失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建公告失败");
        }
    }

    /**
     * 发布公告（通过 Redis Pub/Sub 推送）
     */
    public void publish(Long id) {
        try {
            Announcement announcement = announcementMapper.selectById(id);
            if (announcement == null) {
                log.warn("发布公告失败: 公告不存在, id={}", id);
                return;
            }

            announcement.setStatus(1);
            announcementMapper.updateById(announcement);

            // 通过 Redis Pub/Sub 发布（可选，失败不影响发布）
            try {
                String message = String.format("{\"id\":%d,\"title\":\"%s\",\"type\":\"%s\",\"time\":\"%s\"}",
                        announcement.getId(),
                        announcement.getTitle(),
                        announcement.getType(),
                        LocalDateTime.now().toString());
                redisTemplate.convertAndSend(REDIS_CHANNEL, message);
                redisTemplate.opsForValue().set(REDIS_LATEST_KEY, message);
            } catch (Exception e) {
                log.error("Redis publish failed: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("发布公告失败: {}", e.getMessage(), e);
            throw new RuntimeException("发布公告失败");
        }
    }

    /**
     * 获取公告列表（分页）
     */
    public Page<Announcement> getList(int page, int size, Integer status) {
        try {
            LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
            if (status != null) {
                wrapper.eq(Announcement::getStatus, status);
            }
            wrapper.orderByDesc(Announcement::getCreateTime);
            return announcementMapper.selectPage(new Page<>(page, size), wrapper);
        } catch (Exception e) {
            log.error("获取公告列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取公告列表失败");
        }
    }

    /**
     * 获取已发布的公告列表（用户端）
     */
    public List<Announcement> getPublishedList(int limit) {
        try {
            LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Announcement::getStatus, 1);
            wrapper.and(w -> w.isNull(Announcement::getExpireTime)
                    .or().gt(Announcement::getExpireTime, LocalDateTime.now()));
            wrapper.orderByDesc(Announcement::getCreateTime);
            wrapper.last("LIMIT " + limit);
            return announcementMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("获取已发布公告列表失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取当前有效的维护公告
     */
    public Announcement getActiveMaintenanceAnnouncement() {
        try {
            LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Announcement::getType, "maintenance");
            wrapper.eq(Announcement::getStatus, 1);
            wrapper.and(w -> w.isNull(Announcement::getExpireTime)
                    .or().gt(Announcement::getExpireTime, LocalDateTime.now()));
            wrapper.orderByDesc(Announcement::getCreateTime);
            wrapper.last("LIMIT 1");
            return announcementMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("获取维护公告失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 更新公告
     */
    public void update(Announcement announcement) {
        try {
            announcementMapper.updateById(announcement);
        } catch (Exception e) {
            log.error("更新公告失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新公告失败");
        }
    }

    /**
     * 删除公告
     */
    public void delete(Long id) {
        try {
            announcementMapper.deleteById(id);
        } catch (Exception e) {
            log.error("删除公告失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除公告失败");
        }
    }

    /**
     * 获取公告详情
     */
    public Announcement getById(Long id) {
        try {
            return announcementMapper.selectById(id);
        } catch (Exception e) {
            log.error("获取公告详情失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
