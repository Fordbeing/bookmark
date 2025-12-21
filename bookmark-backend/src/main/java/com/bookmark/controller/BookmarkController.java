package com.bookmark.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.dto.request.BookmarkRequest;
import com.bookmark.dto.response.PageResponse;
import com.bookmark.entity.Bookmark;
import com.bookmark.service.BookmarkService;
import com.bookmark.util.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookmarks")
public class BookmarkController {

    @Autowired
    private BookmarkService bookmarkService;

    @GetMapping
    public Result<PageResponse<Bookmark>> getBookmarkList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Integer isFavorite) {

        Page<Bookmark> pageResult = bookmarkService.getBookmarkList(page, size, categoryId, keyword, sortBy, sortOrder,
                isFavorite);
        PageResponse<Bookmark> response = new PageResponse<>(pageResult.getTotal(), pageResult.getRecords());
        return Result.success(response);
    }

    @GetMapping("/{id}")
    public Result<Bookmark> getBookmarkById(@PathVariable Long id) {
        Bookmark bookmark = bookmarkService.getBookmarkById(id);
        return Result.success(bookmark);
    }

    @PostMapping
    public Result<Bookmark> createBookmark(@Valid @RequestBody BookmarkRequest request) {
        Bookmark bookmark = bookmarkService.createBookmark(request);
        return Result.success("书签创建成功", bookmark);
    }

    @PutMapping("/{id}")
    public Result<Void> updateBookmark(@PathVariable Long id, @RequestBody BookmarkRequest request) {
        bookmarkService.updateBookmark(id, request);
        return Result.success("书签更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteBookmark(@PathVariable Long id) {
        bookmarkService.deleteBookmark(id);
        return Result.success("书签删除成功", null);
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody Map<String, List<Long>> payload) {
        List<Long> ids = payload.get("ids");
        bookmarkService.deleteBatch(ids);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/favorite")
    public Result<Void> updateFavorite(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        Integer isFavorite = payload.get("isFavorite");
        bookmarkService.updateFavorite(id, isFavorite);
        return Result.success("操作成功", null);
    }

    @PutMapping("/{id}/visit")
    public Result<Map<String, Integer>> increaseVisitCount(@PathVariable Long id) {
        bookmarkService.increaseVisitCount(id);
        Bookmark bookmark = bookmarkService.getBookmarkById(id);
        Map<String, Integer> result = new HashMap<>();
        result.put("visitCount", bookmark.getVisitCount());
        return Result.success(result);
    }

    // ========== 回收站相关 API ==========

    /**
     * 获取回收站中的书签
     */
    @GetMapping("/trash")
    public Result<List<Bookmark>> getTrashBookmarks() {
        List<Bookmark> trashBookmarks = bookmarkService.getTrashBookmarks();
        return Result.success(trashBookmarks);
    }

    /**
     * 恢复书签
     */
    @PutMapping("/{id}/restore")
    public Result<Void> restoreBookmark(@PathVariable Long id) {
        bookmarkService.restoreBookmark(id);
        return Result.success("书签已恢复", null);
    }

    /**
     * 永久删除书签
     */
    @DeleteMapping("/{id}/permanent")
    public Result<Void> permanentDeleteBookmark(@PathVariable Long id) {
        bookmarkService.permanentDeleteBookmark(id);
        return Result.success("书签已永久删除", null);
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/trash/clear")
    public Result<Void> clearTrash() {
        bookmarkService.clearTrash();
        return Result.success("回收站已清空", null);
    }

    // ========== 置顶书签相关 API ==========

    /**
     * 置顶书签
     */
    @PutMapping("/{id}/pin")
    public Result<Void> pinBookmark(@PathVariable Long id) {
        bookmarkService.updatePinStatus(id, 1);
        return Result.success("书签已置顶", null);
    }

    /**
     * 取消置顶
     */
    @PutMapping("/{id}/unpin")
    public Result<Void> unpinBookmark(@PathVariable Long id) {
        bookmarkService.updatePinStatus(id, 0);
        return Result.success("已取消置顶", null);
    }

    /**
     * 获取置顶书签列表
     */
    @GetMapping("/pinned")
    public Result<List<Bookmark>> getPinnedBookmarks() {
        List<Bookmark> pinnedBookmarks = bookmarkService.getPinnedBookmarks();
        return Result.success(pinnedBookmarks);
    }

    // ========== 高级搜索 API ==========

    /**
     * 高级搜索（支持多条件筛选）
     */
    @GetMapping("/advanced-search")
    public Result<PageResponse<Bookmark>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer linkStatus,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<Bookmark> pageResult = bookmarkService.advancedSearch(
                keyword, domain, categoryId, startDate, endDate, linkStatus, page, size);
        PageResponse<Bookmark> response = new PageResponse<>(pageResult.getTotal(), pageResult.getRecords());
        return Result.success(response);
    }

    /**
     * 获取失效链接列表
     */
    @GetMapping("/dead-links")
    public Result<List<Bookmark>> getDeadLinks() {
        List<Bookmark> deadLinks = bookmarkService.getDeadLinks();
        return Result.success(deadLinks);
    }
}
