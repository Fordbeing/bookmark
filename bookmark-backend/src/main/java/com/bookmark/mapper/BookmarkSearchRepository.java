package com.bookmark.mapper;

import com.bookmark.entity.BookmarkDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch 书签仓库
 */
@Repository
public interface BookmarkSearchRepository extends ElasticsearchRepository<BookmarkDocument, Long> {

    /**
     * 根据用户ID和关键词搜索
     */
    List<BookmarkDocument> findByUserIdAndTitleContainingOrUserIdAndDescriptionContainingOrUserIdAndTagsContaining(
            Long userId1, String title,
            Long userId2, String description,
            Long userId3, String tags);

    /**
     * 根据用户ID查询所有
     */
    List<BookmarkDocument> findByUserId(Long userId);

    /**
     * 根据用户ID删除所有
     */
    void deleteByUserId(Long userId);
}
