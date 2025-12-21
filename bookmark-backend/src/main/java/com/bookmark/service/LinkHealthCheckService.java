package com.bookmark.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.Bookmark;
import com.bookmark.mapper.BookmarkMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 链接健康检查服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkHealthCheckService {

    private final BookmarkMapper bookmarkMapper;

    private static final int TIMEOUT = 10000; // 10秒超时（增加到10秒）
    private static final int BATCH_SIZE = 50; // 每批检测数量
    private static final int REQUEST_INTERVAL = 300; // 请求间隔（毫秒）

    /**
     * 检测单个链接状态（改进版）
     * 
     * @return LinkCheckResult
     */
    public LinkCheckResult checkLink(String url) {
        if (url == null || url.trim().isEmpty()) {
            return new LinkCheckResult(2, "URL为空", 0);
        }

        try {
            // 规范化 URL
            String normalizedUrl = normalizeUrl(url);

            // 首先尝试 HEAD 请求（更快，节省带宽）
            LinkCheckResult result = doRequest(normalizedUrl, true);

            // 如果 HEAD 请求失败（405/403/500等），回退到 GET 请求
            if (result.getStatus() == 2 || result.getStatus() == 4) {
                int httpCode = result.getHttpCode();
                // 常见的不支持 HEAD 的响应码
                if (httpCode == 405 || httpCode == 403 || httpCode == 500 || httpCode == 0) {
                    log.debug("HEAD请求失败，尝试GET请求: url={}", normalizedUrl);
                    result = doRequest(normalizedUrl, false);
                }
            }

            return result;
        } catch (Exception e) {
            String errorMsg = truncateMessage("检测异常: " + e.getMessage(), 200);
            log.error("链接检测异常: url={}, error={}", url, e.getMessage());
            return new LinkCheckResult(4, errorMsg, 0);
        }
    }

    /**
     * 执行 HTTP 请求
     */
    private LinkCheckResult doRequest(String url, boolean useHead) {
        try {
            HttpRequest request = useHead ? HttpRequest.head(url) : HttpRequest.get(url);

            HttpResponse response = request
                    .timeout(TIMEOUT)
                    .setFollowRedirects(true) // 自动跟随重定向
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .execute();

            int status = response.getStatus();

            if (status >= 200 && status < 300) {
                return new LinkCheckResult(1, "正常", status);
            } else if (status >= 300 && status < 400) {
                String location = response.header("Location");
                String msg = location != null ? "重定向到: " + truncateMessage(location, 100) : "重定向";
                return new LinkCheckResult(3, msg, status);
            } else if (status == 404) {
                return new LinkCheckResult(2, "页面不存在 (404)", status);
            } else if (status == 403) {
                return new LinkCheckResult(1, "访问受限但页面存在 (403)", status); // 403通常意味着页面存在
            } else if (status == 401) {
                return new LinkCheckResult(1, "需要认证但页面存在 (401)", status); // 401也意味着页面存在
            } else if (status >= 500) {
                return new LinkCheckResult(2, "服务器错误 (" + status + ")", status);
            } else {
                return new LinkCheckResult(2, "HTTP错误: " + status, status);
            }
        } catch (cn.hutool.http.HttpException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("timeout")) {
                return new LinkCheckResult(4, "连接超时", 0);
            } else if (msg != null && msg.contains("Connection refused")) {
                return new LinkCheckResult(2, "连接被拒绝", 0);
            } else if (msg != null && msg.contains("UnknownHost")) {
                return new LinkCheckResult(2, "域名无法解析", 0);
            } else if (msg != null && msg.contains("SSLHandshake")) {
                return new LinkCheckResult(4, "SSL证书错误", 0);
            }
            return new LinkCheckResult(4, truncateMessage("连接失败: " + msg, 200), 0);
        } catch (Exception e) {
            return new LinkCheckResult(4, truncateMessage("请求失败: " + e.getMessage(), 200), 0);
        }
    }

    /**
     * 规范化 URL
     */
    private String normalizeUrl(String url) {
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }

    /**
     * 截断消息长度
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null)
            return "";
        if (message.length() <= maxLength)
            return message;
        return message.substring(0, maxLength - 3) + "...";
    }

    /**
     * 检测用户所有书签（异步执行）
     */
    @Async
    public void checkUserBookmarks(Long userId) {
        log.info("开始检测用户书签: userId={}", userId);

        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", userId)
                        .eq("status", 1));

        log.info("用户书签总数: userId={}, total={}", userId, bookmarks.size());

        int checked = 0;
        int errors = 0;
        int deadLinks = 0;

        for (Bookmark bookmark : bookmarks) {
            try {
                LinkCheckResult result = checkLink(bookmark.getUrl());
                bookmark.setLinkStatus(result.getStatus());
                bookmark.setCheckMessage(result.getMessage());
                bookmark.setLastCheckTime(LocalDateTime.now());
                bookmarkMapper.updateById(bookmark);
                checked++;

                if (result.getStatus() == 2) {
                    deadLinks++;
                }

                // 每检测10个输出一次进度
                if (checked % 10 == 0) {
                    log.info("检测进度: userId={}, checked={}/{}", userId, checked, bookmarks.size());
                }

                // 避免请求过快
                Thread.sleep(REQUEST_INTERVAL);
            } catch (Exception e) {
                errors++;
                log.error("检测书签失败: bookmarkId={}, error={}", bookmark.getId(), e.getMessage());
            }
        }

        log.info("用户书签检测完成: userId={}, checked={}, deadLinks={}, errors={}",
                userId, checked, deadLinks, errors);
    }

    /**
     * 定时任务：每天凌晨3点检测超过7天未检测的书签
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyHealthCheck() {
        log.info("开始每日链接健康检查...");

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 查找需要检测的书签（未检测过或检测时间超过7天）
        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .eq("status", 1)
                        .and(w -> w.isNull("last_check_time")
                                .or().lt("last_check_time", sevenDaysAgo))
                        .last("LIMIT " + BATCH_SIZE * 10) // 每次最多检测500个
        );

        log.info("待检测书签数量: {}", bookmarks.size());

        int checked = 0;
        int deadLinks = 0;

        for (Bookmark bookmark : bookmarks) {
            try {
                LinkCheckResult result = checkLink(bookmark.getUrl());
                bookmark.setLinkStatus(result.getStatus());
                bookmark.setCheckMessage(result.getMessage());
                bookmark.setLastCheckTime(LocalDateTime.now());
                bookmarkMapper.updateById(bookmark);

                if (result.getStatus() == 2) {
                    deadLinks++;
                }
                checked++;

                // 避免请求过快
                Thread.sleep(REQUEST_INTERVAL);
            } catch (Exception e) {
                log.error("检测书签失败: bookmarkId={}", bookmark.getId(), e);
            }
        }

        log.info("每日链接健康检查完成: 检测={}, 失效={}", checked, deadLinks);
    }

    /**
     * 手动触发检测（异步）
     */
    @Async
    public void triggerCheck(Long userId) {
        checkUserBookmarks(userId);
    }

    /**
     * 链接检测结果
     */
    @Data
    public static class LinkCheckResult {
        private int status; // 1-正常 2-失效 3-重定向 4-超时
        private String message;
        private int httpCode;

        public LinkCheckResult(int status, String message, int httpCode) {
            this.status = status;
            this.message = message;
            this.httpCode = httpCode;
        }
    }
}
