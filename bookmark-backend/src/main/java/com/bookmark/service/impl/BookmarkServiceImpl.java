package com.bookmark.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.dto.request.BookmarkRequest;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.User;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.service.BookmarkService;
import com.bookmark.service.ActivationCodeService;
import com.bookmark.service.CategoryCacheService;
import com.bookmark.service.SearchService;
import com.bookmark.service.UrlMetadataService;
import com.bookmark.service.UrlMetadataService.UrlMetadata;
import com.bookmark.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class BookmarkServiceImpl implements BookmarkService {

    @Autowired
    private BookmarkMapper bookmarkMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private UrlMetadataService urlMetadataService;

    @Autowired
    private ActivationCodeService activationCodeService;

    @Autowired
    private CategoryCacheService categoryCacheService;

    @Override
    public Bookmark createBookmark(BookmarkRequest request) {
        try {
            User currentUser = userService.getCurrentUser();

            // 检查书签数量限制（使用动态限制）
            int bookmarkLimit = activationCodeService.getUserBookmarkLimit(currentUser.getId());
            Long bookmarkCount = bookmarkMapper.selectCount(
                    new QueryWrapper<Bookmark>()
                            .eq("user_id", currentUser.getId())
                            .eq("status", 1));
            if (bookmarkCount >= bookmarkLimit) {
                throw new RuntimeException("书签数量已达上限（" + bookmarkLimit + "个），请删除部分书签或使用激活码增加额度");
            }

            // 如果标题或描述为空，尝试从 URL 获取元数据
            String title = request.getTitle();
            String description = request.getDescription();
            String iconUrl = null;

            if ((title == null || title.isEmpty()) ||
                    (description == null || description.isEmpty())) {
                try {
                    UrlMetadata metadata = urlMetadataService.fetchMetadata(request.getUrl());
                    if (title == null || title.isEmpty()) {
                        title = metadata.getTitle();
                    }
                    if (description == null || description.isEmpty()) {
                        description = metadata.getDescription();
                    }
                    iconUrl = metadata.getIconUrl();
                } catch (Exception e) {
                    log.warn("获取 URL 元数据失败: {}", e.getMessage());
                }
            }

            // 如果仍然没有标题，使用 URL 的域名
            if (title == null || title.isEmpty()) {
                try {
                    title = request.getUrl().replaceAll("https?://", "").split("/")[0];
                } catch (Exception e) {
                    title = request.getUrl();
                }
            }

            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(currentUser.getId());
            bookmark.setTitle(title);
            bookmark.setUrl(request.getUrl());
            bookmark.setDescription(description);
            bookmark.setCategoryId(request.getCategoryId());
            bookmark.setIsFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : 0);
            bookmark.setVisitCount(0);
            bookmark.setSortOrder(0);
            bookmark.setStatus(1);

            // Convert tags to JSON
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                bookmark.setTags(JSONUtil.toJsonStr(request.getTags()));
            }

            // 设置图标
            if (iconUrl != null && !iconUrl.isEmpty()) {
                bookmark.setIconUrl(iconUrl);
            } else {
                try {
                    String domain = request.getUrl().replaceAll("(https?://[^/]+).*", "$1");
                    bookmark.setIconUrl(domain + "/favicon.ico");
                } catch (Exception e) {
                    bookmark.setIconUrl(null);
                }
            }

            bookmarkMapper.insert(bookmark);

            // 同步到 Elasticsearch
            try {
                searchService.syncBookmark(bookmark);
            } catch (Exception e) {
                log.error("同步书签到 Elasticsearch 失败: {}", e.getMessage(), e);
            }

            // 刷新分类缓存（更新书签数量）
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }

            return bookmark;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建书签失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建书签失败");
        }
    }

    @Override
    public Bookmark updateBookmark(Long id, BookmarkRequest request) {
        try {
            User currentUser = userService.getCurrentUser();
            Bookmark bookmark = bookmarkMapper.selectById(id);

            if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("书签不存在或无权限");
            }

            if (request.getTitle() != null)
                bookmark.setTitle(request.getTitle());
            if (request.getDescription() != null)
                bookmark.setDescription(request.getDescription());
            if (request.getCategoryId() != null)
                bookmark.setCategoryId(request.getCategoryId());
            if (request.getTags() != null)
                bookmark.setTags(JSONUtil.toJsonStr(request.getTags()));
            if (request.getIsFavorite() != null)
                bookmark.setIsFavorite(request.getIsFavorite());

            bookmarkMapper.updateById(bookmark);

            // 同步到 Elasticsearch
            try {
                searchService.syncBookmark(bookmark);
            } catch (Exception e) {
                log.error("同步书签到 Elasticsearch 失败: {}", e.getMessage(), e);
            }

            return bookmark;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新书签失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新书签失败");
        }
    }

    @Override
    public void deleteBookmark(Long id) {
        try {
            User currentUser = userService.getCurrentUser();
            Bookmark bookmark = bookmarkMapper.selectById(id);

            if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("书签不存在或无权限");
            }

            // 使用 CompletableFuture.runAsync 异步执行删除操作
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 先从 Elasticsearch 删除
                    searchService.deleteBookmark(id);
                } catch (Exception e) {
                    log.error("从 Elasticsearch 删除书签失败: {}", e.getMessage(), e);
                }
                // 再从数据库删除
                bookmarkMapper.deleteById(id);
            });

            // 等待异步操作完成
            future.join();

            // 刷新分类缓存（更新书签数量）
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除书签失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除书签失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        try {
            User currentUser = userService.getCurrentUser();
            bookmarkMapper.delete(new QueryWrapper<Bookmark>()
                    .eq("user_id", currentUser.getId())
                    .in("id", ids));

            // 刷新分类缓存（更新书签数量）
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("批量删除书签失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量删除书签失败");
        }
    }

    @Override
    public Bookmark getBookmarkById(Long id) {
        try {
            User currentUser = userService.getCurrentUser();
            Bookmark bookmark = bookmarkMapper.selectById(id);

            if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("书签不存在或无权限");
            }

            return bookmark;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取书签详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取书签详情失败");
        }
    }

    @Override
    public Page<Bookmark> getBookmarkList(Integer page, Integer size, Long categoryId,
            String keyword, String sortBy, String sortOrder, Integer isFavorite) {
        try {
            User currentUser = userService.getCurrentUser();

            Page<Bookmark> pageParam = new Page<>(page, size);
            QueryWrapper<Bookmark> wrapper = new QueryWrapper<>();

            // 1. 基础过滤条件：属于当前用户
            wrapper.eq("user_id", currentUser.getId());

            // 2. 新增条件：只查找 status 为 1 的数据
            wrapper.eq("status", 1);

            // 3. 筛选：分类
            if (categoryId != null) {
                wrapper.eq("category_id", categoryId);
            }

            // 4. 筛选：收藏夹
            if (isFavorite != null && isFavorite == 1) {
                wrapper.eq("is_favorite", 1);
            }

            // 5. 筛选：关键字模糊查询
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like("title", keyword).or().like("description", keyword).or().like("url", keyword));
            }

            // 6. 排序逻辑：置顶书签优先
            wrapper.orderByDesc("is_pinned"); // 置顶书签优先显示
            if (sortBy != null && !sortBy.isEmpty()) {
                if ("desc".equalsIgnoreCase(sortOrder)) {
                    wrapper.orderByDesc(sortBy);
                } else {
                    wrapper.orderByAsc(sortBy);
                }
            } else {
                wrapper.orderByDesc("create_time");
            }

            return bookmarkMapper.selectPage(pageParam, wrapper);
        } catch (Exception e) {
            log.error("获取书签列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取书签列表失败");
        }
    }

    @Override
    public void updateFavorite(Long id, Integer isFavorite) {
        try {
            User currentUser = userService.getCurrentUser();
            Bookmark bookmark = bookmarkMapper.selectById(id);

            if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("书签不存在或无权限");
            }

            bookmark.setIsFavorite(isFavorite);
            bookmarkMapper.updateById(bookmark);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新收藏状态失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新收藏状态失败");
        }
    }

    @Override
    public void increaseVisitCount(Long id) {
        try {
            User currentUser = userService.getCurrentUser();
            Bookmark bookmark = bookmarkMapper.selectById(id);

            if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("书签不存在或无权限");
            }

            bookmark.setVisitCount(bookmark.getVisitCount() + 1);
            bookmarkMapper.updateById(bookmark);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("增加访问次数失败: {}", e.getMessage(), e);
        }
    }

    // ========== 回收站相关方法 ==========

    @Override
    public List<Bookmark> getTrashBookmarks() {
        try {
            User currentUser = userService.getCurrentUser();
            // 使用原生 SQL 查询，绕过 @TableLogic
            return bookmarkMapper.selectTrashByUserId(currentUser.getId());
        } catch (Exception e) {
            log.error("获取回收站书签失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void restoreBookmark(Long id) {
        try {
            User currentUser = userService.getCurrentUser();
            // 使用原生 SQL 恢复，绕过 @TableLogic
            bookmarkMapper.restoreById(id, currentUser.getId());

            // 同步到ES索引（恢复后需要重建索引）
            try {
                Bookmark restored = bookmarkMapper.selectById(id);
                if (restored != null) {
                    searchService.syncBookmark(restored);
                    log.info("已同步恢复的书签到ES索引: bookmarkId={}", id);
                }
            } catch (Exception e) {
                log.error("同步ES索引失败: {}", e.getMessage(), e);
            }

            // 刷新分类缓存（更新书签数量）
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("恢复书签失败: {}", e.getMessage(), e);
            throw new RuntimeException("恢复书签失败");
        }
    }

    @Override
    public void permanentDeleteBookmark(Long id) {
        try {
            User currentUser = userService.getCurrentUser();
            // 使用原生 SQL 永久删除，绕过 @TableLogic
            bookmarkMapper.permanentDeleteById(id, currentUser.getId());
        } catch (Exception e) {
            log.error("永久删除书签失败: {}", e.getMessage(), e);
            throw new RuntimeException("永久删除书签失败");
        }
    }

    @Override
    public void clearTrash() {
        try {
            User currentUser = userService.getCurrentUser();
            // 使用原生 SQL 清空回收站，绕过 @TableLogic
            bookmarkMapper.clearTrashByUserId(currentUser.getId());
        } catch (Exception e) {
            log.error("清空回收站失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空回收站失败");
        }
    }

    // ========== 置顶书签相关方法 ==========

    @Override
    public void updatePinStatus(Long id, Integer isPinned) {
        try {
            User currentUser = userService.getCurrentUser();
            Bookmark bookmark = bookmarkMapper.selectOne(
                    new QueryWrapper<Bookmark>()
                            .eq("id", id)
                            .eq("user_id", currentUser.getId()));
            if (bookmark == null) {
                throw new RuntimeException("书签不存在");
            }
            bookmark.setIsPinned(isPinned);
            bookmarkMapper.updateById(bookmark);
        } catch (Exception e) {
            log.error("更新置顶状态失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新置顶状态失败");
        }
    }

    @Override
    public List<Bookmark> getPinnedBookmarks() {
        try {
            User currentUser = userService.getCurrentUser();
            return bookmarkMapper.selectList(
                    new QueryWrapper<Bookmark>()
                            .eq("user_id", currentUser.getId())
                            .eq("is_pinned", 1)
                            .eq("status", 1)
                            .orderByDesc("update_time"));
        } catch (Exception e) {
            log.error("获取置顶书签失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ========== 高级搜索相关方法 ==========

    @Override
    public Page<Bookmark> advancedSearch(String keyword, String domain, Long categoryId,
            String startDate, String endDate, Integer linkStatus, Integer page, Integer size) {
        try {
            User currentUser = userService.getCurrentUser();
            Page<Bookmark> pageParam = new Page<>(page, size);
            QueryWrapper<Bookmark> wrapper = new QueryWrapper<>();

            // 基础条件
            wrapper.eq("user_id", currentUser.getId());
            wrapper.eq("status", 1);

            // 关键字搜索
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like("title", keyword)
                        .or().like("description", keyword)
                        .or().like("url", keyword));
            }

            // 域名筛选
            if (domain != null && !domain.isEmpty()) {
                wrapper.like("url", domain);
            }

            // 分类筛选
            if (categoryId != null) {
                wrapper.eq("category_id", categoryId);
            }

            // 日期范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge("create_time", startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le("create_time", endDate + " 23:59:59");
            }

            // 链接状态筛选
            if (linkStatus != null) {
                wrapper.eq("link_status", linkStatus);
            }

            // 排序：置顶优先，然后按创建时间
            wrapper.orderByDesc("is_pinned");
            wrapper.orderByDesc("create_time");

            return bookmarkMapper.selectPage(pageParam, wrapper);
        } catch (Exception e) {
            log.error("高级搜索失败: {}", e.getMessage(), e);
            throw new RuntimeException("搜索失败");
        }
    }

    @Override
    public List<Bookmark> getDeadLinks() {
        try {
            User currentUser = userService.getCurrentUser();
            return bookmarkMapper.selectList(
                    new QueryWrapper<Bookmark>()
                            .eq("user_id", currentUser.getId())
                            .eq("status", 1)
                            .eq("link_status", 2) // 2 = 失效
                            .orderByDesc("last_check_time"));
        } catch (Exception e) {
            log.error("获取失效链接失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
