package com.bookmark.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.entity.AdminLog;
import com.bookmark.entity.User;
import com.bookmark.mapper.UserMapper;
import com.bookmark.service.AdminLogService;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员日志控制器
 */
@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {

    @Autowired
    private AdminLogService adminLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取操作日志列表
     */
    @GetMapping
    public Result<Map<String, Object>> getLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Page<AdminLog> pageResult = adminLogService.getLogList(page, size, actionType, targetType, startDate, endDate,
                keyword);

        // 添加管理员用户名
        List<Map<String, Object>> records = new ArrayList<>();
        for (AdminLog log : pageResult.getRecords()) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", log.getId());
            record.put("adminId", log.getAdminId());
            record.put("actionType", log.getActionType());
            record.put("targetType", log.getTargetType());
            record.put("targetId", log.getTargetId());
            record.put("oldValue", log.getOldValue());
            record.put("newValue", log.getNewValue());
            record.put("ipAddress", log.getIpAddress());
            record.put("createTime", log.getCreateTime());

            // 获取管理员用户名
            User admin = userMapper.selectById(log.getAdminId());
            record.put("adminName", admin != null ? admin.getUsername() : "未知");

            records.add(record);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());

        return Result.success(result);
    }

    /**
     * 获取日志详情
     */
    @GetMapping("/{id}")
    public Result<AdminLog> getLogDetail(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        // 简单返回，可以扩展
        return Result.success(null);
    }
}
