package com.bookmark.controller;

import com.bookmark.entity.Announcement;
import com.bookmark.service.AnnouncementService;
import com.bookmark.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端公告控制器
 */
@Slf4j
@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 获取已发布的公告列表（用户端）
     */
    @GetMapping
    public Result<List<Announcement>> getPublishedAnnouncements(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Announcement> list = announcementService.getPublishedList(limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取公告列表失败: {}", e.getMessage(), e);
            return Result.error("获取公告列表失败");
        }
    }

    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    public Result<Announcement> getAnnouncementDetail(@PathVariable Long id) {
        try {
            Announcement announcement = announcementService.getById(id);
            if (announcement == null || announcement.getStatus() != 1) {
                return Result.error("公告不存在");
            }
            return Result.success(announcement);
        } catch (Exception e) {
            log.error("获取公告详情失败: {}", e.getMessage(), e);
            return Result.error("获取公告详情失败");
        }
    }
}
