package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.BookmarkDocument;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.mapper.BookmarkSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * ES 同步定时任务
 * 每30分钟检查并同步数据库中未同步到 ES 的书签
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchSyncTask {

    private final BookmarkMapper bookmarkMapper;
    private final BookmarkSearchRepository searchRepository;

    /**
     * 每30分钟执行一次同步
     * 检查数据库中的书签是否都已同步到 ES
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟 = 30 * 60 * 1000 毫秒
    public void syncBookmarksToElasticsearch() {
        log.info("开始执行 ES 同步定时任务...");

        try {
            // 1. 获取数据库中所有正常状态的书签
            QueryWrapper<Bookmark> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", 1); // 只同步正常状态的书签
            List<Bookmark> dbBookmarks = bookmarkMapper.selectList(queryWrapper);

            if (dbBookmarks.isEmpty()) {
                log.info("数据库中没有需要同步的书签");
                return;
            }

            // 2. 获取 ES 中已有的书签 ID
            Set<Long> esBookmarkIds = StreamSupport
                    .stream(searchRepository.findAll().spliterator(), false)
                    .map(BookmarkDocument::getId)
                    .collect(Collectors.toSet());

            // 3. 找出数据库中有但 ES 中没有的书签
            List<Bookmark> bookmarksToSync = dbBookmarks.stream()
                    .filter(bookmark -> !esBookmarkIds.contains(bookmark.getId()))
                    .collect(Collectors.toList());

            if (bookmarksToSync.isEmpty()) {
                log.info("所有书签已同步，无需更新");
                return;
            }

            // 4. 同步缺失的书签到 ES
            List<BookmarkDocument> documentsToSave = bookmarksToSync.stream()
                    .map(BookmarkDocument::fromBookmark)
                    .collect(Collectors.toList());

            searchRepository.saveAll(documentsToSave);

            log.info("ES 同步完成，新同步 {} 条书签", documentsToSave.size());

        } catch (Exception e) {
            log.error("ES 同步定时任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发全量同步
     * 清空 ES 并重新同步所有书签
     */
    public void fullSync() {
        log.info("开始执行全量 ES 同步...");

        try {
            // 1. 清空 ES 索引
            searchRepository.deleteAll();

            // 2. 获取所有正常状态的书签
            QueryWrapper<Bookmark> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", 1);
            List<Bookmark> allBookmarks = bookmarkMapper.selectList(queryWrapper);

            // 3. 同步到 ES
            List<BookmarkDocument> documents = allBookmarks.stream()
                    .map(BookmarkDocument::fromBookmark)
                    .collect(Collectors.toList());

            searchRepository.saveAll(documents);

            log.info("全量 ES 同步完成，共同步 {} 条书签", documents.size());

        } catch (Exception e) {
            log.error("全量 ES 同步失败: {}", e.getMessage(), e);
        }
    }
}
