package com.bookmark.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.dto.request.BookmarkRequest;
import com.bookmark.entity.Bookmark;

import java.util.List;

public interface BookmarkService {
    Bookmark createBookmark(BookmarkRequest request);

    Bookmark updateBookmark(Long id, BookmarkRequest request);

    void deleteBookmark(Long id);

    void deleteBatch(List<Long> ids);

    Bookmark getBookmarkById(Long id);

    Page<Bookmark> getBookmarkList(Integer page, Integer size, Long categoryId, String keyword, String sortBy,
            String sortOrder);

    void updateFavorite(Long id, Integer isFavorite);

    void increaseVisitCount(Long id);

    // 回收站相关方法
    List<Bookmark> getTrashBookmarks();

    void restoreBookmark(Long id);

    void permanentDeleteBookmark(Long id);

    void clearTrash();
}
