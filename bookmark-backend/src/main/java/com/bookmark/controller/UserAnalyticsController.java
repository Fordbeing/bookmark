package com.bookmark.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.User;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户统计分析控制器
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class UserAnalyticsController {

    private final BookmarkMapper bookmarkMapper;
    private final UserService userService;

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("https?://([^/]+)");

    /**
     * 获取统计概览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser.getId();

        // 总书签数
        long totalBookmarks = bookmarkMapper.selectCount(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", userId)
                        .eq("status", 1));

        // 本周新增
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long weeklyAdded = bookmarkMapper.selectCount(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", userId)
                        .eq("status", 1)
                        .ge("create_time", weekAgo));

        // 失效链接数
        long deadLinks = bookmarkMapper.selectCount(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", userId)
                        .eq("status", 1)
                        .eq("link_status", 2));

        // 置顶书签数
        long pinnedCount = bookmarkMapper.selectCount(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", userId)
                        .eq("status", 1)
                        .eq("is_pinned", 1));

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalBookmarks", totalBookmarks);
        overview.put("weeklyAdded", weeklyAdded);
        overview.put("deadLinks", deadLinks);
        overview.put("pinnedCount", pinnedCount);

        return Result.success(overview);
    }

    /**
     * 获取域名分布统计
     */
    @GetMapping("/domain-distribution")
    public Result<List<Map<String, Object>>> getDomainDistribution() {
        User currentUser = userService.getCurrentUser();

        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", currentUser.getId())
                        .eq("status", 1)
                        .select("url"));

        // 统计域名分布
        Map<String, Long> domainCount = bookmarks.stream()
                .map(b -> extractDomain(b.getUrl()))
                .filter(d -> d != null && !d.isEmpty())
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        // 排序并取前10
        List<Map<String, Object>> result = domainCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("domain", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取时间线统计（按月份）
     */
    @GetMapping("/timeline")
    public Result<List<Map<String, Object>>> getTimeline() {
        User currentUser = userService.getCurrentUser();

        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", currentUser.getId())
                        .eq("status", 1)
                        .select("create_time")
                        .orderByAsc("create_time"));

        // 按月份统计
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> monthlyCount = bookmarks.stream()
                .filter(b -> b.getCreateTime() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getCreateTime().format(formatter),
                        LinkedHashMap::new,
                        Collectors.counting()));

        // 确保最近12个月都有数据
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            String month = now.minusMonths(i).format(formatter);
            Map<String, Object> item = new HashMap<>();
            item.put("month", month);
            item.put("count", monthlyCount.getOrDefault(month, 0L));
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取常用书签TOP10
     */
    @GetMapping("/top-visited")
    public Result<List<Bookmark>> getTopVisited() {
        User currentUser = userService.getCurrentUser();

        List<Bookmark> topBookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", currentUser.getId())
                        .eq("status", 1)
                        .gt("visit_count", 0)
                        .orderByDesc("visit_count")
                        .last("LIMIT 10"));

        return Result.success(topBookmarks);
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        if (url == null)
            return null;
        Matcher matcher = DOMAIN_PATTERN.matcher(url);
        if (matcher.find()) {
            String domain = matcher.group(1);
            // 移除 www. 前缀
            if (domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            return domain;
        }
        return null;
    }
}
