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
import com.bookmark.service.SearchService;
import com.bookmark.service.UrlMetadataService;
import com.bookmark.service.UrlMetadataService.UrlMetadata;
import com.bookmark.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    @Override
    public Bookmark createBookmark(BookmarkRequest request) {
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
                // 获取元数据失败，使用默认值
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
        searchService.syncBookmark(bookmark);

        return bookmark;
    }

    @Override
    public Bookmark updateBookmark(Long id, BookmarkRequest request) {
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
        searchService.syncBookmark(bookmark);

        return bookmark;
    }

    @Override
    public void deleteBookmark(Long id) {
        User currentUser = userService.getCurrentUser();
        Bookmark bookmark = bookmarkMapper.selectById(id);

        if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("书签不存在或无权限");
        }

        // 使用 CompletableFuture.runAsync 异步执行删除操作
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // 先从 Elasticsearch 删除
            searchService.deleteBookmark(id);
            // 再从数据库删除
            bookmarkMapper.deleteById(id);
        });

        // 等待异步操作完成，如果有异常则抛出
        try {
            future.join(); // join() 会等待完成并抛出未检查异常
        } catch (Exception e) {
            throw new RuntimeException("删除书签失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        User currentUser = userService.getCurrentUser();
        bookmarkMapper.delete(new QueryWrapper<Bookmark>()
                .eq("user_id", currentUser.getId())
                .in("id", ids));
    }

    @Override
    public Bookmark getBookmarkById(Long id) {
        User currentUser = userService.getCurrentUser();
        Bookmark bookmark = bookmarkMapper.selectById(id);

        if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("书签不存在或无权限");
        }

        return bookmark;
    }

    @Override
    public Page<Bookmark> getBookmarkList(Integer page, Integer size, Long categoryId,
            String keyword, String sortBy, String sortOrder) {
        User currentUser = userService.getCurrentUser();

        Page<Bookmark> pageParam = new Page<>(page, size);
        QueryWrapper<Bookmark> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUser.getId());

        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword).or().like("url", keyword));
        }

        // Sorting
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
    }

    @Override
    public void updateFavorite(Long id, Integer isFavorite) {
        User currentUser = userService.getCurrentUser();
        Bookmark bookmark = bookmarkMapper.selectById(id);

        if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("书签不存在或无权限");
        }

        bookmark.setIsFavorite(isFavorite);
        bookmarkMapper.updateById(bookmark);
    }

    @Override
    public void increaseVisitCount(Long id) {
        User currentUser = userService.getCurrentUser();
        Bookmark bookmark = bookmarkMapper.selectById(id);

        if (bookmark == null || !bookmark.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("书签不存在或无权限");
        }

        bookmark.setVisitCount(bookmark.getVisitCount() + 1);
        bookmarkMapper.updateById(bookmark);
    }

    // ========== 回收站相关方法 ==========

    @Override
    public List<Bookmark> getTrashBookmarks() {
        User currentUser = userService.getCurrentUser();
        // 直接查询 status=0 的书签（逻辑删除的）
        return bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", currentUser.getId())
                        .eq("status", 0)
                        .orderByDesc("update_time"));
    }

    @Override
    public void restoreBookmark(Long id) {
        User currentUser = userService.getCurrentUser();
        // 使用原生 SQL 更新，因为 MyBatis-Plus 逻辑删除会过滤掉 status=0 的记录
        bookmarkMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Bookmark>()
                        .eq("id", id)
                        .eq("user_id", currentUser.getId())
                        .set("status", 1));
    }

    @Override
    public void permanentDeleteBookmark(Long id) {
        User currentUser = userService.getCurrentUser();
        // 真正删除记录
        bookmarkMapper.delete(
                new QueryWrapper<Bookmark>()
                        .eq("id", id)
                        .eq("user_id", currentUser.getId())
                        .eq("status", 0));
    }

    @Override
    public void clearTrash() {
        User currentUser = userService.getCurrentUser();
        // 删除所有逻辑删除的书签
        bookmarkMapper.delete(
                new QueryWrapper<Bookmark>()
                        .eq("user_id", currentUser.getId())
                        .eq("status", 0));
    }
}
