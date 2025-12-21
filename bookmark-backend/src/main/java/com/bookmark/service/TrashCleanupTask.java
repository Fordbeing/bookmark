package com.bookmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 回收站自动清理任务
 * 删除超过3天的已删除书签
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrashCleanupTask {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 每天凌晨4点自动清理超过3天的回收站书签
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupExpiredTrash() {
        try {
            log.info("开始清理过期的回收站书签...");

            // 删除 status=0 且 update_time 超过3天的书签
            // 使用原生SQL绕过MyBatis-Plus的逻辑删除保护
            int deleted = jdbcTemplate.update(
                    "DELETE FROM bookmark WHERE status = 0 AND update_time < DATE_SUB(NOW(), INTERVAL 3 DAY)");

            log.info("回收站清理完成，共删除 {} 条过期书签", deleted);
        } catch (Exception e) {
            log.error("清理回收站失败", e);
        }
    }

    /**
     * 手动触发清理（供测试或管理员调用）
     */
    public int cleanupNow() {
        log.info("手动触发回收站清理...");
        int deleted = jdbcTemplate.update(
                "DELETE FROM bookmark WHERE status = 0 AND update_time < DATE_SUB(NOW(), INTERVAL 3 DAY)");
        log.info("手动清理完成，删除 {} 条书签", deleted);
        return deleted;
    }
}
