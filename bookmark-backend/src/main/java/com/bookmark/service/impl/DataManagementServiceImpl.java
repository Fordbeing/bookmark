package com.bookmark.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.dto.request.ImportRequest;
import com.bookmark.dto.response.ExportResponse;
import com.bookmark.dto.response.ImportResponse;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.Category;
import com.bookmark.entity.Tag;
import com.bookmark.entity.User;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.mapper.CategoryMapper;
import com.bookmark.mapper.TagMapper;
import com.bookmark.service.DataManagementService;
import com.bookmark.service.SearchService;
import com.bookmark.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataManagementServiceImpl implements DataManagementService {

    private final BookmarkMapper bookmarkMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserService userService;
    private final SearchService searchService;

    // 正则表达式用于解析 HTML 书签
    private static final Pattern FOLDER_PATTERN = Pattern.compile(
            "<DT><H3[^>]*>([^<]+)</H3>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOKMARK_PATTERN = Pattern.compile(
            "<DT><A\\s+HREF=\"([^\"]+)\"[^>]*(?:ADD_DATE=\"(\\d+)\")?[^>]*>([^<]*)</A>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DL_END = Pattern.compile("</DL>", Pattern.CASE_INSENSITIVE);

    @Override
    public ExportResponse exportData() {
        ExportResponse response = new ExportResponse();

        try {
            User currentUser = userService.getCurrentUser();
            Long userId = currentUser.getId();

            response.setExportTime(LocalDateTime.now());

            // 获取所有书签
            List<Bookmark> bookmarks = bookmarkMapper.selectList(
                    new QueryWrapper<Bookmark>()
                            .eq("user_id", userId)
                            .eq("status", 1)
                            .orderByAsc("create_time"));
            response.setBookmarks(bookmarks);
            response.setTotalBookmarks(bookmarks.size());

            // 获取所有分类
            List<Category> categories = categoryMapper.selectList(
                    new QueryWrapper<Category>()
                            .eq("user_id", userId)
                            .orderByAsc("sort_order"));
            response.setCategories(categories);
            response.setTotalCategories(categories.size());

            // 获取所有标签
            List<Tag> tags = tagMapper.selectList(
                    new QueryWrapper<Tag>()
                            .eq("user_id", userId)
                            .orderByAsc("name"));
            response.setTags(tags);
            response.setTotalTags(tags.size());

            log.info("导出数据成功: userId={}, bookmarks={}, categories={}, tags={}",
                    userId, bookmarks.size(), categories.size(), tags.size());
        } catch (Exception e) {
            log.error("导出数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出数据失败");
        }

        return response;
    }

    @Override
    @Transactional
    public ImportResponse importData(ImportRequest request) {
        try {
            String importType = request.getImportType().toUpperCase();

            switch (importType) {
                case "CHROME":
                case "EDGE":
                    return importFromBrowser(request.getContent());
                case "JSON":
                    return importFromJson(request.getContent());
                default:
                    ImportResponse errorResponse = new ImportResponse();
                    errorResponse.addMessage("不支持的导入类型: " + importType);
                    return errorResponse;
            }
        } catch (Exception e) {
            log.error("导入数据失败: {}", e.getMessage(), e);
            ImportResponse errorResponse = new ImportResponse();
            errorResponse.addMessage("导入失败: " + e.getMessage());
            return errorResponse;
        }
    }

    @Override
    @Transactional
    public void clearAllData() {
        try {
            User currentUser = userService.getCurrentUser();
            Long userId = currentUser.getId();

            // 删除所有书签（包括 ES）
            List<Bookmark> bookmarks = bookmarkMapper.selectList(
                    new QueryWrapper<Bookmark>().eq("user_id", userId));
            for (Bookmark bookmark : bookmarks) {
                try {
                    searchService.deleteBookmark(bookmark.getId());
                } catch (Exception e) {
                    log.error("从 ES 删除书签失败: id={}, error={}", bookmark.getId(), e.getMessage());
                }
            }
            bookmarkMapper.delete(new QueryWrapper<Bookmark>().eq("user_id", userId));

            // 删除所有分类
            categoryMapper.delete(new QueryWrapper<Category>().eq("user_id", userId));

            // 删除所有标签
            tagMapper.delete(new QueryWrapper<Tag>().eq("user_id", userId));

            log.info("清除用户所有数据: userId={}", userId);
        } catch (Exception e) {
            log.error("清除数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("清除数据失败");
        }
    }

    @Override
    @Transactional
    public boolean clearAllDataWithPasswordVerification(String password) {
        try {
            User currentUser = userService.getCurrentUser();

            // 验证密码
            if (!org.springframework.security.crypto.bcrypt.BCrypt.checkpw(password, currentUser.getPassword())) {
                log.warn("清除数据密码验证失败: userId={}", currentUser.getId());
                return false;
            }

            // 密码正确，执行清除
            clearAllData();
            return true;
        } catch (Exception e) {
            log.error("清除数据验证失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从浏览器 HTML 格式导入书签
     */
    private ImportResponse importFromBrowser(String htmlContent) {
        ImportResponse response = new ImportResponse();

        try {
            User currentUser = userService.getCurrentUser();
            Long userId = currentUser.getId();

            // 验证格式
            if (!htmlContent.contains("<!DOCTYPE NETSCAPE-Bookmark-file-1>") &&
                    !htmlContent.toUpperCase().contains("NETSCAPE-BOOKMARK-FILE")) {
                response.addMessage("格式错误：不是有效的浏览器书签 HTML 文件");
                return response;
            }

            // 获取现有 URL 用于检查重复
            Set<String> existingUrls = new HashSet<>();
            bookmarkMapper.selectList(
                    new QueryWrapper<Bookmark>()
                            .eq("user_id", userId)
                            .eq("status", 1))
                    .forEach(b -> existingUrls.add(normalizeUrl(b.getUrl())));

            // 获取现有分类（按名称索引）
            Map<String, Category> categoryMap = new HashMap<>();
            categoryMapper.selectList(
                    new QueryWrapper<Category>().eq("user_id", userId))
                    .forEach(c -> categoryMap.put(c.getName().toLowerCase(), c));

            // 解析并导入
            List<Bookmark> importedBookmarks = new ArrayList<>();
            parseAndImportHtml(htmlContent, userId, null, existingUrls, categoryMap, response, importedBookmarks);

            // 批量同步到 ES
            if (!importedBookmarks.isEmpty()) {
                try {
                    searchService.syncAllBookmarks(importedBookmarks);
                } catch (Exception e) {
                    log.error("批量同步到 ES 失败: {}", e.getMessage(), e);
                }
            }

            log.info("浏览器书签导入完成: userId={}, success={}, skipped={}, failed={}, categories={}",
                    userId, response.getSuccessCount(), response.getSkippedCount(),
                    response.getFailedCount(), response.getCategoriesCreated());
        } catch (Exception e) {
            log.error("浏览器书签导入失败: {}", e.getMessage(), e);
            response.addMessage("导入失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 递归解析 HTML 并导入书签
     */
    private void parseAndImportHtml(String html, Long userId, Long categoryId,
            Set<String> existingUrls, Map<String, Category> categoryMap,
            ImportResponse response, List<Bookmark> importedBookmarks) {
        // 简化的解析：按行处理
        String[] lines = html.split("\n");
        Long currentCategoryId = categoryId;
        Stack<Long> categoryStack = new Stack<>();
        if (categoryId != null) {
            categoryStack.push(categoryId);
        }

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            try {
                // 检测文件夹开始
                Matcher folderMatcher = FOLDER_PATTERN.matcher(line);
                if (folderMatcher.find()) {
                    String folderName = folderMatcher.group(1).trim();
                    // 跳过特殊文件夹名称
                    if (isSpecialFolder(folderName)) {
                        continue;
                    }

                    // 查找或创建分类
                    Category category = categoryMap.get(folderName.toLowerCase());
                    if (category == null) {
                        category = createCategory(userId, folderName);
                        categoryMap.put(folderName.toLowerCase(), category);
                        response.incrementCategoriesCreated();
                    }
                    categoryStack.push(category.getId());
                    currentCategoryId = category.getId();
                    continue;
                }

                // 检测 DL 结束（退出当前文件夹）
                if (DL_END.matcher(line).find() && !categoryStack.isEmpty()) {
                    categoryStack.pop();
                    currentCategoryId = categoryStack.isEmpty() ? null : categoryStack.peek();
                }

                // 检测书签
                Matcher bookmarkMatcher = BOOKMARK_PATTERN.matcher(line);
                if (bookmarkMatcher.find()) {
                    String url = bookmarkMatcher.group(1);
                    String addDateStr = bookmarkMatcher.group(2);
                    String title = bookmarkMatcher.group(3).trim();

                    // 验证 URL
                    if (url == null || url.isEmpty()) {
                        response.incrementSkipped();
                        continue;
                    }

                    // 检查重复
                    String normalizedUrl = normalizeUrl(url);
                    if (existingUrls.contains(normalizedUrl)) {
                        response.incrementSkipped();
                        continue;
                    }

                    try {
                        Bookmark bookmark = createBookmark(userId, url, title, addDateStr, currentCategoryId);
                        existingUrls.add(normalizedUrl);
                        importedBookmarks.add(bookmark);
                        response.incrementSuccess();
                    } catch (Exception e) {
                        response.incrementFailed();
                        response.addMessage("导入失败: " + title + " - " + e.getMessage());
                        log.error("导入书签失败: title={}, error={}", title, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("解析行失败: line={}, error={}", i, e.getMessage());
            }
        }
    }

    /**
     * 判断是否为特殊文件夹（不需要创建分类）
     */
    private boolean isSpecialFolder(String name) {
        String lower = name.toLowerCase();
        return lower.equals("bookmarks") ||
                lower.equals("书签栏") ||
                lower.equals("书签") ||
                lower.equals("其他书签") ||
                lower.equals("other bookmarks") ||
                lower.equals("bookmarks bar") ||
                lower.equals("favorites") ||
                lower.equals("favorites bar");
    }

    /**
     * 创建分类
     */
    private Category createCategory(Long userId, String name) {
        try {
            Category category = new Category();
            category.setUserId(userId);
            category.setName(name);
            category.setColor(generateRandomColor());
            category.setSortOrder(0);
            categoryMapper.insert(category);
            return category;
        } catch (Exception e) {
            log.error("创建分类失败: name={}, error={}", name, e.getMessage(), e);
            throw new RuntimeException("创建分类失败");
        }
    }

    /**
     * 创建书签
     */
    private Bookmark createBookmark(Long userId, String url, String title, String addDateStr, Long categoryId) {
        try {
            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setUrl(url);
            bookmark.setTitle(title.isEmpty() ? url : title);
            bookmark.setCategoryId(categoryId);
            bookmark.setIsFavorite(0);
            bookmark.setVisitCount(0);
            bookmark.setSortOrder(0);
            bookmark.setStatus(1);

            // 尝试设置创建时间
            if (addDateStr != null && !addDateStr.isEmpty()) {
                try {
                    long timestamp = Long.parseLong(addDateStr);
                    LocalDateTime createTime = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                    bookmark.setCreateTime(createTime);
                } catch (Exception e) {
                    log.warn("解析时间戳失败: {}", addDateStr);
                }
            }

            // 设置图标
            try {
                String domain = url.replaceAll("(https?://[^/]+).*", "$1");
                bookmark.setIconUrl(domain + "/favicon.ico");
            } catch (Exception e) {
                bookmark.setIconUrl(null);
            }

            bookmarkMapper.insert(bookmark);
            return bookmark;
        } catch (Exception e) {
            log.error("创建书签失败: url={}, error={}", url, e.getMessage(), e);
            throw new RuntimeException("创建书签失败");
        }
    }

    /**
     * 从本应用 JSON 格式导入
     */
    private ImportResponse importFromJson(String jsonContent) {
        ImportResponse response = new ImportResponse();

        try {
            User currentUser = userService.getCurrentUser();
            Long userId = currentUser.getId();

            JSONObject json = JSONUtil.parseObj(jsonContent);

            // 验证格式
            if (!json.containsKey("version") && !json.containsKey("bookmarks")) {
                response.addMessage("格式错误：不是有效的书签 JSON 文件");
                return response;
            }

            // 获取现有 URL
            Set<String> existingUrls = new HashSet<>();
            bookmarkMapper.selectList(
                    new QueryWrapper<Bookmark>()
                            .eq("user_id", userId)
                            .eq("status", 1))
                    .forEach(b -> existingUrls.add(normalizeUrl(b.getUrl())));

            // 导入分类（建立旧ID到新分类的映射）
            Map<Long, Long> categoryIdMap = new HashMap<>();
            JSONArray categories = json.getJSONArray("categories");
            if (categories != null) {
                for (int i = 0; i < categories.size(); i++) {
                    try {
                        JSONObject cat = categories.getJSONObject(i);
                        Long oldId = cat.getLong("id");
                        String name = cat.getStr("name");

                        // 检查是否已存在
                        Category existing = categoryMapper.selectOne(
                                new QueryWrapper<Category>()
                                        .eq("user_id", userId)
                                        .eq("name", name));

                        if (existing != null) {
                            categoryIdMap.put(oldId, existing.getId());
                        } else {
                            Category newCat = new Category();
                            newCat.setUserId(userId);
                            newCat.setName(name);
                            newCat.setColor(cat.getStr("color", generateRandomColor()));
                            newCat.setIcon(cat.getStr("icon"));
                            newCat.setSortOrder(cat.getInt("sortOrder", 0));
                            categoryMapper.insert(newCat);
                            categoryIdMap.put(oldId, newCat.getId());
                            response.incrementCategoriesCreated();
                        }
                    } catch (Exception e) {
                        log.error("导入分类失败: index={}, error={}", i, e.getMessage());
                    }
                }
            }

            // 导入书签
            List<Bookmark> importedBookmarks = new ArrayList<>();
            JSONArray bookmarks = json.getJSONArray("bookmarks");
            if (bookmarks != null) {
                for (int i = 0; i < bookmarks.size(); i++) {
                    try {
                        JSONObject bm = bookmarks.getJSONObject(i);
                        String url = bm.getStr("url");

                        if (url == null || url.isEmpty()) {
                            response.incrementSkipped();
                            continue;
                        }

                        if (existingUrls.contains(normalizeUrl(url))) {
                            response.incrementSkipped();
                            continue;
                        }

                        Bookmark bookmark = new Bookmark();
                        bookmark.setUserId(userId);
                        bookmark.setUrl(url);
                        bookmark.setTitle(bm.getStr("title", url));
                        bookmark.setDescription(bm.getStr("description"));
                        bookmark.setIconUrl(bm.getStr("iconUrl"));
                        bookmark.setTags(bm.getStr("tags"));

                        // 映射分类ID
                        Long oldCategoryId = bm.getLong("categoryId");
                        if (oldCategoryId != null && categoryIdMap.containsKey(oldCategoryId)) {
                            bookmark.setCategoryId(categoryIdMap.get(oldCategoryId));
                        }

                        bookmark.setIsFavorite(bm.getInt("isFavorite", 0));
                        bookmark.setVisitCount(0); // 重置访问计数
                        bookmark.setSortOrder(bm.getInt("sortOrder", 0));
                        bookmark.setStatus(1);

                        bookmarkMapper.insert(bookmark);
                        existingUrls.add(normalizeUrl(url));
                        importedBookmarks.add(bookmark);
                        response.incrementSuccess();
                    } catch (Exception e) {
                        response.incrementFailed();
                        log.error("导入书签失败: index={}, error={}", i, e.getMessage());
                    }
                }
            }

            // 批量同步到 ES
            if (!importedBookmarks.isEmpty()) {
                try {
                    searchService.syncAllBookmarks(importedBookmarks);
                } catch (Exception e) {
                    log.error("批量同步到 ES 失败: {}", e.getMessage(), e);
                }
            }

            log.info("JSON 导入完成: userId={}, success={}, skipped={}, failed={}, categories={}",
                    userId, response.getSuccessCount(), response.getSkippedCount(),
                    response.getFailedCount(), response.getCategoriesCreated());

        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage(), e);
            response.addMessage("JSON 解析错误: " + e.getMessage());
        }

        return response;
    }

    /**
     * 规范化 URL 用于去重比较
     */
    private String normalizeUrl(String url) {
        if (url == null)
            return "";
        return url.toLowerCase()
                .replaceAll("^https?://", "")
                .replaceAll("^www\\.", "")
                .replaceAll("/$", "");
    }

    /**
     * 生成随机颜色
     */
    private String generateRandomColor() {
        String[] colors = { "#2563eb", "#9333ea", "#16a34a", "#dc2626", "#ea580c", "#0891b2" };
        return colors[new Random().nextInt(colors.length)];
    }
}
