package com.bookmark.service;

import com.bookmark.entity.Bookmark;
import com.bookmark.entity.BookmarkDocument;
import com.bookmark.mapper.BookmarkSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

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
            String searchText = keyword.trim();
            String wildcardPattern = "*" + searchText.toLowerCase() + "*";

            // 构建原生 Elasticsearch 查询
            // 1. 用户ID必须匹配（term query）
            Query userQuery = TermQuery.of(t -> t.field("userId").value(userId))._toQuery();

            // 2. 标题/描述/标签使用 multi_match 查询
            Query textQuery = MultiMatchQuery.of(m -> m
                    .query(searchText)
                    .fields("title^3", "description", "tags")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO"))._toQuery();

            // 3. URL 使用 wildcard 查询（支持部分匹配）
            Query urlQuery = co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery.of(w -> w
                    .field("url")
                    .value(wildcardPattern)
                    .caseInsensitive(true))._toQuery();

            // 4. 文本查询或URL查询任一匹配即可（should）
            Query searchQuery = BoolQuery.of(b -> b
                    .should(textQuery)
                    .should(urlQuery)
                    .minimumShouldMatch("1"))._toQuery();

            // 5. 最终查询：用户必须匹配 AND 搜索条件匹配
            Query boolQuery = BoolQuery.of(b -> b
                    .must(userQuery)
                    .must(searchQuery))._toQuery();

            NativeQuery query = NativeQuery.builder()
                    .withQuery(boolQuery)
                    .build();

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
