package com.bookmark.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.entity.Announcement;
import com.bookmark.entity.User;
import com.bookmark.service.AdminLogService;
import com.bookmark.service.AnnouncementService;
import com.bookmark.service.SystemConfigService;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台 - 系统设置控制器
 */
@RestController
@RequestMapping("/admin/system")
public class AdminSystemController {

    @Autowired
    private SystemConfigService configService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminLogService adminLogService;

    /**
     * 获取系统配置
     */
    @GetMapping("/config")
    public Result<Map<String, String>> getConfig() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Map<String, String> config = configService.getAllConfig();
        return Result.success(config);
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/config")
    public Result<String> updateConfig(@RequestBody Map<String, String> config) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        configService.updateConfigs(config);

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "修改系统配置", "系统配置", null, null, config.toString());

        return Result.success("配置已更新");
    }

    /**
     * 获取公告列表
     */
    @GetMapping("/announcements")
    public Result<Map<String, Object>> getAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Page<Announcement> result = announcementService.getList(page, size, status);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("current", result.getCurrent());

        return Result.success(data);
    }

    /**
     * 创建公告
     */
    @PostMapping("/announcements")
    public Result<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        announcement.setCreatedBy(currentUser.getId());
        Announcement created = announcementService.create(announcement);

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "创建公告", "公告", created.getId(), null, created.getTitle());

        return Result.success(created);
    }

    /**
     * 发布公告
     */
    @PostMapping("/announcements/{id}/publish")
    public Result<String> publishAnnouncement(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        announcementService.publish(id);

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "发布公告", "公告", id);

        return Result.success("公告已发布");
    }

    /**
     * 更新公告
     */
    @PutMapping("/announcements/{id}")
    public Result<String> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        announcement.setId(id);
        announcementService.update(announcement);

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "更新公告", "公告", id, null, announcement.getTitle());

        return Result.success("公告已更新");
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/announcements/{id}")
    public Result<String> deleteAnnouncement(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        announcementService.delete(id);

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "删除公告", "公告", id);

        return Result.success("公告已删除");
    }

    /**
     * 开启/关闭维护模式
     */
    @PostMapping("/maintenance")
    public Result<String> setMaintenanceMode(@RequestBody Map<String, Object> body) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Boolean enabled = (Boolean) body.get("enabled");
        String message = (String) body.get("message");

        configService.setConfig("maintenance_mode", enabled ? "1" : "0");
        if (message != null) {
            configService.setConfig("maintenance_message", message);
        }

        // 记录操作日志
        adminLogService.log(currentUser.getId(), enabled ? "开启维护模式" : "关闭维护模式", "系统配置", null);

        return Result.success(enabled ? "已开启维护模式" : "已关闭维护模式");
    }
}
