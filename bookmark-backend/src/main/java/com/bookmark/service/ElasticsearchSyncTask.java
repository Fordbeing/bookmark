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
     * 双向同步：MySQL → ES（添加缺失），ES → MySQL（删除孤儿文档）
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟 = 30 * 60 * 1000 毫秒
    public void syncBookmarksToElasticsearch() {
        log.info("开始执行 ES 双向同步定时任务...");

        try {
            // 1. 获取数据库中所有正常状态的书签 ID
            QueryWrapper<Bookmark> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", 1).select("id"); // 只查询 ID 字段以提高性能
            List<Bookmark> dbBookmarks = bookmarkMapper.selectList(queryWrapper);

            Set<Long> dbBookmarkIds = dbBookmarks.stream()
                    .map(Bookmark::getId)
                    .collect(Collectors.toSet());

            // 2. 获取 ES 中所有书签文档
            Iterable<BookmarkDocument> esDocuments = searchRepository.findAll();
            Set<Long> esBookmarkIds = StreamSupport
                    .stream(esDocuments.spliterator(), false)
                    .map(BookmarkDocument::getId)
                    .collect(Collectors.toSet());

            // === 第一阶段：MySQL → ES，同步缺失的书签 ===
            List<Long> idsToSyncToEs = dbBookmarkIds.stream()
                    .filter(id -> !esBookmarkIds.contains(id))
                    .collect(Collectors.toList());

            if (!idsToSyncToEs.isEmpty()) {
                // 重新查询完整的书签信息
                QueryWrapper<Bookmark> fullQueryWrapper = new QueryWrapper<>();
                fullQueryWrapper.eq("status", 1).in("id", idsToSyncToEs);
                List<Bookmark> bookmarksToSync = bookmarkMapper.selectList(fullQueryWrapper);

                List<BookmarkDocument> documentsToSave = bookmarksToSync.stream()
                        .map(BookmarkDocument::fromBookmark)
                        .collect(Collectors.toList());

                searchRepository.saveAll(documentsToSave);
                log.info("MySQL → ES 同步完成，新增 {} 条书签", documentsToSave.size());
            } else {
                log.info("MySQL → ES：所有书签已同步");
            }

            // === 第二阶段：ES → MySQL，删除 ES 中的孤儿文档 ===
            List<Long> orphanedEsIds = esBookmarkIds.stream()
                    .filter(id -> !dbBookmarkIds.contains(id))
                    .collect(Collectors.toList());

            if (!orphanedEsIds.isEmpty()) {
                searchRepository.deleteAllById(orphanedEsIds);
                log.info("ES → MySQL 清理完成，删除 {} 条孤儿文档", orphanedEsIds.size());
            } else {
                log.info("ES → MySQL：无孤儿文档需要清理");
            }

            log.info("ES 双向同步任务执行完成");

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
