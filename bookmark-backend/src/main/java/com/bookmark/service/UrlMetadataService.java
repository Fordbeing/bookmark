package com.bookmark.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL 元数据抓取服务
 */
@Slf4j
@Service
public class UrlMetadataService {

    private static final int TIMEOUT = 5000; // 5秒超时
    private static final int MAX_CONTENT_LENGTH = 512 * 1024; // 限制读取 512KB

    // 正则表达式
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_DESC_PATTERN = Pattern.compile(
            "<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_DESC_PATTERN2 = Pattern.compile(
            "<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+name=[\"']description[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OG_DESC_PATTERN = Pattern.compile(
            "<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LINK_ICON_PATTERN = Pattern.compile(
            "<link[^>]+rel=[\"'](?:shortcut icon|icon)[\"'][^>]+href=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LINK_ICON_PATTERN2 = Pattern.compile(
            "<link[^>]+href=[\"']([^\"']+)[\"'][^>]+rel=[\"'](?:shortcut icon|icon)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 获取 URL 的元数据
     */
    public UrlMetadata fetchMetadata(String url) {
        UrlMetadata metadata = new UrlMetadata();
        metadata.setUrl(url);

        try {
            // 确保 URL 格式正确
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            String domain = extractDomain(url);

            // 发送 HTTP 请求
            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .execute();

            if (!response.isOk()) {
                log.warn("获取 URL 元数据失败, status={}, url={}", response.getStatus(), url);
                metadata.setIconUrl(domain + "/favicon.ico");
                return metadata;
            }

            // 限制读取的内容长度
            String html = response.body();
            if (html.length() > MAX_CONTENT_LENGTH) {
                html = html.substring(0, MAX_CONTENT_LENGTH);
            }

            // 解析标题
            Matcher titleMatcher = TITLE_PATTERN.matcher(html);
            if (titleMatcher.find()) {
                String title = titleMatcher.group(1).trim();
                // 清理 HTML 实体
                title = decodeHtmlEntities(title);
                metadata.setTitle(title);
            }

            // 解析描述
            String description = extractDescription(html);
            if (description != null && !description.isEmpty()) {
                metadata.setDescription(decodeHtmlEntities(description));
            }

            // 解析图标
            String iconUrl = extractIconUrl(html, url, domain);
            metadata.setIconUrl(iconUrl);

            log.debug("URL 元数据获取成功: url={}, title={}", url, metadata.getTitle());

        } catch (Exception e) {
            log.warn("获取 URL 元数据异常: url={}, error={}", url, e.getMessage());
            // 设置默认图标
            try {
                String domain = extractDomain(url);
                metadata.setIconUrl(domain + "/favicon.ico");
            } catch (Exception ex) {
                // ignore
            }
        }

        return metadata;
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        try {
            return url.replaceAll("(https?://[^/]+).*", "$1");
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * 提取描述
     */
    private String extractDescription(String html) {
        // 尝试多种 meta description 格式
        Matcher matcher = META_DESC_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        matcher = META_DESC_PATTERN2.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 尝试 Open Graph description
        matcher = OG_DESC_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 提取图标 URL
     */
    private String extractIconUrl(String html, String url, String domain) {
        // 尝试从 link 标签获取
        Matcher matcher = LINK_ICON_PATTERN.matcher(html);
        if (matcher.find()) {
            return resolveUrl(matcher.group(1), url, domain);
        }

        matcher = LINK_ICON_PATTERN2.matcher(html);
        if (matcher.find()) {
            return resolveUrl(matcher.group(1), url, domain);
        }

        // 使用默认 favicon
        return domain + "/favicon.ico";
    }

    /**
     * 解析相对 URL 为绝对 URL
     */
    private String resolveUrl(String iconUrl, String pageUrl, String domain) {
        if (iconUrl == null || iconUrl.isEmpty()) {
            return domain + "/favicon.ico";
        }

        iconUrl = iconUrl.trim();

        // 已经是绝对 URL
        if (iconUrl.startsWith("http://") || iconUrl.startsWith("https://")) {
            return iconUrl;
        }

        // 协议相对 URL
        if (iconUrl.startsWith("//")) {
            return "https:" + iconUrl;
        }

        // 根路径
        if (iconUrl.startsWith("/")) {
            return domain + iconUrl;
        }

        // 相对路径
        return domain + "/" + iconUrl;
    }

    /**
     * 解码 HTML 实体
     */
    private String decodeHtmlEntities(String text) {
        if (text == null)
            return null;
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/")
                .trim();
    }

    /**
     * URL 元数据
     */
    @Data
    public static class UrlMetadata {
        private String url;
        private String title;
        private String description;
        private String iconUrl;
    }
}
