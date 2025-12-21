package com.bookmark.controller;

import com.bookmark.entity.Bookmark;
import com.bookmark.entity.User;
import com.bookmark.service.BookmarkService;
import com.bookmark.service.LinkHealthCheckService;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 链接健康检查控制器
 */
@RestController
@RequestMapping("/bookmarks/health")
@RequiredArgsConstructor
public class LinkHealthController {

    private final LinkHealthCheckService linkHealthCheckService;
    private final BookmarkService bookmarkService;
    private final UserService userService;

    /**
     * 手动触发链接检测
     */
    @PostMapping("/check")
    public Result<Map<String, String>> triggerCheck() {
        User currentUser = userService.getCurrentUser();
        linkHealthCheckService.triggerCheck(currentUser.getId());

        Map<String, String> response = new HashMap<>();
        response.put("message", "链接检测已开始，请稍后刷新查看结果");
        return Result.success("检测任务已启动", response);
    }

    /**
     * 检测单个链接
     */
    @PostMapping("/check/{id}")
    public Result<Map<String, Object>> checkSingleLink(@PathVariable Long id) {
        Bookmark bookmark = bookmarkService.getBookmarkById(id);
        if (bookmark == null) {
            return Result.error(404, "书签不存在");
        }

        LinkHealthCheckService.LinkCheckResult result = linkHealthCheckService.checkLink(bookmark.getUrl());

        Map<String, Object> response = new HashMap<>();
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        response.put("httpCode", result.getHttpCode());

        return Result.success(response);
    }

    /**
     * 获取失效链接列表
     */
    @GetMapping("/dead-links")
    public Result<List<Bookmark>> getDeadLinks() {
        List<Bookmark> deadLinks = bookmarkService.getDeadLinks();
        return Result.success(deadLinks);
    }

    /**
     * 获取健康检查统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getHealthStats() {
        List<Bookmark> deadLinks = bookmarkService.getDeadLinks();

        Map<String, Object> stats = new HashMap<>();
        stats.put("deadLinksCount", deadLinks.size());

        return Result.success(stats);
    }
}
