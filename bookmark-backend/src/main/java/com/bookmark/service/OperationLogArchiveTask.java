package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmark.entity.AdminLog;
import com.bookmark.mapper.AdminLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * 操作日志自动归档与清理任务
 * - 每3天凌晨3点归档超过3天的日志
 * - 每天凌晨4点清理超过30天的归档文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogArchiveTask {

    private final AdminLogMapper adminLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${bookmark.log.archive-path:./logs/archive}")
    private String archivePath;

    @Value("${bookmark.log.archive-days:3}")
    private int archiveDays;

    @Value("${bookmark.log.cleanup-days:30}")
    private int cleanupDays;

    /**
     * 每3天凌晨3点执行归档任务
     * 将超过3天的日志导出为 JSON 并压缩为 .gz 文件，然后从数据库删除
     */
    @Scheduled(cron = "0 0 3 */3 * ?")
    public void archiveLogs() {
        try {
            log.info("开始归档操作日志...");

            // 计算归档截止时间（3天前）
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(archiveDays);

            // 查询需要归档的日志
            List<AdminLog> logsToArchive = adminLogMapper.selectList(
                    new LambdaQueryWrapper<AdminLog>()
                            .lt(AdminLog::getCreateTime, cutoffTime)
                            .orderByAsc(AdminLog::getCreateTime));

            if (logsToArchive.isEmpty()) {
                log.info("没有需要归档的日志");
                return;
            }

            log.info("找到 {} 条需要归档的日志", logsToArchive.size());

            // 确保归档目录存在
            Path archiveDir = Paths.get(archivePath);
            Files.createDirectories(archiveDir);

            // 生成归档文件名
            String fileName = String.format("admin_logs_%s.json.gz",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            Path archiveFile = archiveDir.resolve(fileName);

            // 将日志转换为 JSON 并压缩
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(logsToArchive);

            try (OutputStream fos = Files.newOutputStream(archiveFile);
                    GZIPOutputStream gzos = new GZIPOutputStream(fos);
                    OutputStreamWriter writer = new OutputStreamWriter(gzos, "UTF-8")) {
                writer.write(jsonContent);
            }

            log.info("日志已归档到: {}", archiveFile.toAbsolutePath());

            // 从数据库删除已归档的日志
            int deleted = jdbcTemplate.update(
                    "DELETE FROM admin_log WHERE create_time < ?",
                    java.sql.Timestamp.valueOf(cutoffTime));

            log.info("归档完成，共归档 {} 条日志，删除 {} 条数据库记录",
                    logsToArchive.size(), deleted);

        } catch (Exception e) {
            log.error("归档操作日志失败", e);
        }
    }

    /**
     * 每天凌晨4点检查并清理超过30天的归档文件
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupOldArchives() {
        try {
            log.info("开始清理过期的归档文件...");

            Path archiveDir = Paths.get(archivePath);
            if (!Files.exists(archiveDir)) {
                log.info("归档目录不存在，跳过清理");
                return;
            }

            // 计算清理截止时间
            long cutoffMillis = System.currentTimeMillis() - (cleanupDays * 24L * 60 * 60 * 1000);
            int cleanedCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(archiveDir, "*.gz")) {
                for (Path file : stream) {
                    long lastModified = Files.getLastModifiedTime(file).toMillis();
                    if (lastModified < cutoffMillis) {
                        Files.delete(file);
                        log.info("已删除过期归档文件: {}", file.getFileName());
                        cleanedCount++;
                    }
                }
            }

            log.info("清理完成，共删除 {} 个过期归档文件", cleanedCount);

        } catch (Exception e) {
            log.error("清理过期归档文件失败", e);
        }
    }

    /**
     * 手动触发归档（供管理员调用）
     */
    public void archiveNow() {
        log.info("手动触发日志归档...");
        archiveLogs();
    }

    /**
     * 手动触发清理（供管理员调用）
     */
    public void cleanupNow() {
        log.info("手动触发归档文件清理...");
        cleanupOldArchives();
    }

    /**
     * 获取归档状态信息
     */
    public String getArchiveStatus() {
        try {
            Path archiveDir = Paths.get(archivePath);
            if (!Files.exists(archiveDir)) {
                return "归档目录不存在";
            }

            long fileCount = 0;
            long totalSize = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(archiveDir, "*.gz")) {
                for (Path file : stream) {
                    fileCount++;
                    totalSize += Files.size(file);
                }
            }

            return String.format("归档文件数: %d, 总大小: %.2f MB",
                    fileCount, totalSize / (1024.0 * 1024.0));

        } catch (Exception e) {
            return "获取状态失败: " + e.getMessage();
        }
    }
}
