package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.entity.LoginHistory;
import com.bookmark.mapper.LoginHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录历史服务
 */
@Slf4j
@Service
public class LoginHistoryService {

    @Autowired
    private LoginHistoryMapper loginHistoryMapper;

    /**
     * 记录登录历史
     */
    public void recordLogin(Long userId, HttpServletRequest request, boolean success, String failReason) {
        try {
            LoginHistory history = new LoginHistory();
            history.setUserId(userId);
            history.setIpAddress(getClientIp(request));
            history.setDevice(parseDevice(request.getHeader("User-Agent")));
            history.setBrowser(parseBrowser(request.getHeader("User-Agent")));
            history.setStatus(success ? 1 : 0);
            history.setFailReason(failReason);
            // location 需要通过 IP 解析，暂时留空
            history.setLocation("-");

            loginHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录登录历史失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取用户登录历史（分页）
     */
    public Map<String, Object> getLoginHistory(Long userId, int page, int size) {
        Page<LoginHistory> pageResult = loginHistoryMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<LoginHistory>()
                        .eq(LoginHistory::getUserId, userId)
                        .orderByDesc(LoginHistory::getCreateTime));

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
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
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个IP取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 解析设备信息
     */
    private String parseDevice(String userAgent) {
        if (userAgent == null)
            return "未知";
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "移动设备";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "平板";
        } else if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac")) {
            return "Mac";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        }
        return "其他";
    }

    /**
     * 解析浏览器信息
     */
    private String parseBrowser(String userAgent) {
        if (userAgent == null)
            return "未知";
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("edg")) {
            return "Edge";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        }
        return "其他";
    }
}
