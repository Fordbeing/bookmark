package com.bookmark.dto.response;

import com.bookmark.entity.Bookmark;
import com.bookmark.entity.Category;
import com.bookmark.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出响应 DTO
 */
@Data
public class ExportResponse {
    /**
     * 导出版本号
     */
    private String version = "1.0";

    /**
     * 导出时间
     */
    private LocalDateTime exportTime;

    /**
     * 书签列表
     */
    private List<Bookmark> bookmarks;

    /**
     * 分类列表
     */
    private List<Category> categories;

    /**
     * 标签列表
     */
    private List<Tag> tags;

    /**
     * 书签总数
     */
    private int totalBookmarks;

    /**
     * 分类总数
     */
    private int totalCategories;

    /**
     * 标签总数
     */
    private int totalTags;
}
