package com.bookmark.service;

import com.bookmark.entity.Bookmark;
import com.bookmark.entity.BookmarkDocument;
import com.bookmark.mapper.BookmarkSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final BookmarkSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 同步书签到 Elasticsearch
     */
    public void syncBookmark(Bookmark bookmark) {
        try {
            BookmarkDocument doc = BookmarkDocument.fromBookmark(bookmark);
            searchRepository.save(doc);
            log.debug("书签已同步到ES: id={}", bookmark.getId());
        } catch (Exception e) {
            log.error("同步书签到ES失败: id={}, error={}", bookmark.getId(), e.getMessage());
        }
    }

    /**
     * 从 Elasticsearch 删除书签
     */
    public void deleteBookmark(Long id) {
        try {
            searchRepository.deleteById(id);
            log.debug("书签已从ES删除: id={}", id);
        } catch (Exception e) {
            log.error("从ES删除书签失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * 搜索书签（用户隔离）
     */
    public List<BookmarkDocument> search(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 使用 Criteria 构建查询
            Criteria criteria = new Criteria("userId").is(userId)
                    .and(
                            new Criteria("title").contains(keyword)
                                    .or("description").contains(keyword)
                                    .or("tags").contains(keyword)
                                    .or("url").contains(keyword));

            CriteriaQuery query = new CriteriaQuery(criteria);
            SearchHits<BookmarkDocument> searchHits = elasticsearchOperations.search(query, BookmarkDocument.class);

            return searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 批量同步书签
     */
    public void syncAllBookmarks(List<Bookmark> bookmarks) {
        try {
            List<BookmarkDocument> docs = bookmarks.stream()
                    .map(BookmarkDocument::fromBookmark)
                    .collect(Collectors.toList());
            searchRepository.saveAll(docs);
            log.info("批量同步书签到ES: count={}", docs.size());
        } catch (Exception e) {
            log.error("批量同步书签失败: error={}", e.getMessage());
        }
    }
}
