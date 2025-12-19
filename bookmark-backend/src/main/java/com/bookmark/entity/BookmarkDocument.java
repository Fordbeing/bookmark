package com.bookmark.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * Elasticsearch 书签文档
 */
@Data
@Document(indexName = "bookmarks")
public class BookmarkDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String url;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String tags;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Integer)
    private Integer isFavorite;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss || uuuu-MM-dd || epoch_millis")
    private LocalDateTime createTime;

    /**
     * 从 Bookmark 实体转换
     */
    public static BookmarkDocument fromBookmark(Bookmark bookmark) {
        BookmarkDocument doc = new BookmarkDocument();
        doc.setId(bookmark.getId());
        doc.setUserId(bookmark.getUserId());
        doc.setTitle(bookmark.getTitle());
        doc.setUrl(bookmark.getUrl());
        doc.setDescription(bookmark.getDescription());
        doc.setTags(bookmark.getTags());
        doc.setCategoryId(bookmark.getCategoryId());
        doc.setIsFavorite(bookmark.getIsFavorite());
        doc.setCreateTime(bookmark.getCreateTime());
        return doc;
    }
}
