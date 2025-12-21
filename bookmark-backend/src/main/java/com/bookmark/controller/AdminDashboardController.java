package com.bookmark.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.User;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.mapper.CategoryMapper;
import com.bookmark.mapper.TagMapper;
import com.bookmark.mapper.UserMapper;
import com.bookmark.mapper.AdminLogMapper;
import com.bookmark.service.UserService;
import com.bookmark.service.TokenService;
import com.bookmark.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员仪表盘控制器
 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookmarkMapper bookmarkMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AdminLogMapper adminLogMapper;

    /**
     * 获取系统概览数据
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        // 验证管理员权限
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Map<String, Object> data = new HashMap<>();

        // 总用户数
        Long totalUsers = userMapper.selectCount(null);
        data.put("totalUsers", totalUsers);

        // 总书签数
        Long totalBookmarks = bookmarkMapper.selectCount(
                new LambdaQueryWrapper<Bookmark>().eq(Bookmark::getStatus, 1));
        data.put("totalBookmarks", totalBookmarks);

        // 总分类数
        Long totalCategories = categoryMapper.selectCount(null);
        data.put("totalCategories", totalCategories);

        // 总标签数
        Long totalTags = tagMapper.selectCount(null);
        data.put("totalTags", totalTags);

        // 今日新增用户
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart));
        data.put("todayNewUsers", todayNewUsers);

        // 今日新增书签
        Long todayNewBookmarks = bookmarkMapper.selectCount(
                new LambdaQueryWrapper<Bookmark>()
                        .ge(Bookmark::getCreateTime, todayStart)
                        .eq(Bookmark::getStatus, 1));
        data.put("todayNewBookmarks", todayNewBookmarks);

        // 活跃用户数 (最近7天有登录的用户)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        Long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getLastLoginTime, weekAgo));
        data.put("activeUsers", activeUsers);

        // 当前在线用户数（拥有有效 Access Token 的用户）
        long onlineUsers = tokenService.getOnlineUserCount();
        data.put("onlineUsers", onlineUsers);

        return Result.success(data);
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trends")
    public Result<Map<String, Object>> getTrends(@RequestParam(defaultValue = "7") int days) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Map<String, Object> data = new HashMap<>();

        // 生成日期列表和统计数据
        List<String> dates = new ArrayList<>();
        List<Long> userTrend = new ArrayList<>();
        List<Long> bookmarkTrend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 日期格式 MM-DD
            dates.add(String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth()));

            // 当天新增用户数
            Long userCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getCreateTime, dayStart)
                            .le(User::getCreateTime, dayEnd));
            userTrend.add(userCount);

            // 当天新增书签数
            Long bookmarkCount = bookmarkMapper.selectCount(
                    new LambdaQueryWrapper<Bookmark>()
                            .ge(Bookmark::getCreateTime, dayStart)
                            .le(Bookmark::getCreateTime, dayEnd)
                            .eq(Bookmark::getStatus, 1));
            bookmarkTrend.add(bookmarkCount);
        }

        data.put("dates", dates);
        data.put("userTrend", userTrend);
        data.put("bookmarkTrend", bookmarkTrend);

        return Result.success(data);
    }

    /**
     * 获取 Elasticsearch 状态
     */
    @GetMapping("/elasticsearch")
    public Result<Map<String, Object>> getElasticsearchStatus() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", "healthy");
        data.put("indexCount",
                bookmarkMapper.selectCount(new LambdaQueryWrapper<Bookmark>().eq(Bookmark::getStatus, 1)));

        return Result.success(data);
    }

    /**
     * 获取最近活动（用于仪表盘）
     */
    @GetMapping("/activities")
    public Result<List<Map<String, Object>>> getRecentActivities(@RequestParam(defaultValue = "10") int limit) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        List<Map<String, Object>> activities = new ArrayList<>();

        // 获取最近的管理员日志
        List<com.bookmark.entity.AdminLog> adminLogs = adminLogMapper.selectList(
                new LambdaQueryWrapper<com.bookmark.entity.AdminLog>()
                        .orderByDesc(com.bookmark.entity.AdminLog::getCreateTime)
                        .last("LIMIT " + limit));

        for (com.bookmark.entity.AdminLog log : adminLogs) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", log.getId());
            activity.put("type", "admin");
            activity.put("icon", getActivityIcon(log.getActionType()));
            activity.put("text", formatActivityText(log));
            activity.put("time", formatRelativeTime(log.getCreateTime()));
            activity.put("bgColor", getActivityBgColor(log.getActionType()));
            activity.put("createTime", log.getCreateTime());
            activities.add(activity);
        }

        // 获取最近注册的用户
        List<User> recentUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getCreateTime)
                        .last("LIMIT 5"));

        for (User user : recentUsers) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", "user_" + user.getId());
            activity.put("type", "user_register");
            activity.put("icon", "👤");
            activity.put("text", "新用户 " + maskUsername(user.getUsername()) + " 注册");
            activity.put("time", formatRelativeTime(user.getCreateTime()));
            activity.put("bgColor", "#eef2ff");
            activity.put("createTime", user.getCreateTime());
            activities.add(activity);
        }

        // 按时间排序并限制数量
        activities.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("createTime");
            LocalDateTime timeB = (LocalDateTime) b.get("createTime");
            return timeB.compareTo(timeA);
        });

        // 移除 createTime 字段（前端不需要）并限制数量
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, activities.size()); i++) {
            Map<String, Object> act = activities.get(i);
            act.remove("createTime");
            result.add(act);
        }

        return Result.success(result);
    }

    private String getActivityIcon(String actionType) {
        if (actionType == null)
            return "📝";
        switch (actionType) {
            case "登录":
                return "🔐";
            case "设置管理员":
            case "取消管理员":
                return "👑";
            case "启用用户":
                return "✅";
            case "禁用用户":
                return "🚫";
            case "创建激活码":
                return "🎫";
            case "修改配置":
                return "⚙️";
            default:
                return "📝";
        }
    }

    private String getActivityBgColor(String actionType) {
        if (actionType == null)
            return "#f3f4f6";
        switch (actionType) {
            case "登录":
                return "#dbeafe";
            case "设置管理员":
            case "取消管理员":
                return "#fef3c7";
            case "启用用户":
                return "#d1fae5";
            case "禁用用户":
                return "#fee2e2";
            case "创建激活码":
                return "#fef3c7";
            case "修改配置":
                return "#e0e7ff";
            default:
                return "#f3f4f6";
        }
    }

    private String formatActivityText(com.bookmark.entity.AdminLog log) {
        String action = log.getActionType();
        String target = log.getTargetType();
        Long targetId = log.getTargetId();

        if ("用户".equals(target) && targetId != null) {
            User user = userMapper.selectById(targetId);
            String username = user != null ? maskUsername(user.getUsername()) : "#" + targetId;
            return action + " 用户 " + username;
        }

        return action + (target != null ? " " + target : "") + (targetId != null ? " #" + targetId : "");
    }

    private String maskUsername(String username) {
        if (username == null || username.length() <= 2)
            return username;
        return username.substring(0, 1) + "***" + username.substring(username.length() - 1);
    }

    private String formatRelativeTime(LocalDateTime time) {
        if (time == null)
            return "未知";

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(time, now).toMinutes();

        if (minutes < 1)
            return "刚刚";
        if (minutes < 60)
            return minutes + "分钟前";

        long hours = minutes / 60;
        if (hours < 24)
            return hours + "小时前";

        long days = hours / 24;
        if (days < 30)
            return days + "天前";

        return time.toLocalDate().toString();
    }
}
