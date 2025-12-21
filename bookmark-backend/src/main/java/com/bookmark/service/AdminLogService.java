package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.entity.AdminLog;
import com.bookmark.mapper.AdminLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 管理员日志服务
 */
@Slf4j
@Service
public class AdminLogService {

    @Autowired
    private AdminLogMapper adminLogMapper;

    /**
     * 记录操作日志
     */
    public void log(Long adminId, String actionType, String targetType, Long targetId, String oldValue,
            String newValue) {
        try {
            AdminLog adminLog = new AdminLog();
            adminLog.setAdminId(adminId);
            adminLog.setActionType(actionType);
            adminLog.setTargetType(targetType);
            adminLog.setTargetId(targetId);
            adminLog.setOldValue(oldValue);
            adminLog.setNewValue(newValue);
            adminLog.setCreateTime(LocalDateTime.now());

            // 获取 IP 和 User-Agent
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    adminLog.setIpAddress(getClientIp(request));
                    adminLog.setUserAgent(request.getHeader("User-Agent"));
                }
            } catch (Exception e) {
                log.warn("获取请求信息失败: {}", e.getMessage());
            }

            adminLogMapper.insert(adminLog);
        } catch (Exception e) {
            log.error("管理员日志保存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 简化版日志记录
     */
    public void log(Long adminId, String actionType, String targetType, Long targetId) {
        log(adminId, actionType, targetType, targetId, null, null);
    }

    /**
     * 获取日志列表（支持多种筛选条件）
     */
    public Page<AdminLog> getLogList(int page, int size, String actionType,
            String targetType, String startDate, String endDate, String keyword) {
        try {
            LambdaQueryWrapper<AdminLog> wrapper = new LambdaQueryWrapper<>();

            if (actionType != null && !actionType.isEmpty()) {
                wrapper.eq(AdminLog::getActionType, actionType);
            }

            if (targetType != null && !targetType.isEmpty()) {
                wrapper.eq(AdminLog::getTargetType, targetType);
            }

            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(AdminLog::getCreateTime, java.time.LocalDate.parse(startDate).atStartOfDay());
            }

            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(AdminLog::getCreateTime, java.time.LocalDate.parse(endDate).atTime(23, 59, 59));
            }

            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w
                        .like(AdminLog::getActionType, keyword)
                        .or()
                        .like(AdminLog::getTargetType, keyword)
                        .or()
                        .like(AdminLog::getOldValue, keyword)
                        .or()
                        .like(AdminLog::getNewValue, keyword));
            }

            wrapper.orderByDesc(AdminLog::getCreateTime);

            return adminLogMapper.selectPage(new Page<>(page, size), wrapper);
        } catch (Exception e) {
            log.error("获取日志列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取日志列表失败");
        }
    }

    /**
     * 获取日志列表（向后兼容的简化版本）
     */
    public Page<AdminLog> getLogList(int page, int size, String actionType) {
        return getLogList(page, size, actionType, null, null, null, null);
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
