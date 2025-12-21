package com.bookmark.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.SharedBookmarks;
import com.bookmark.entity.User;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.mapper.SharedBookmarksMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 批量书签分享服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharedBookmarksService {

    private final SharedBookmarksMapper sharedBookmarksMapper;
    private final BookmarkMapper bookmarkMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建批量分享
     */
    public SharedBookmarks createBatchShare(List<Long> bookmarkIds, String title, String password, Integer expireDays) {
        User currentUser = userService.getCurrentUser();

        if (bookmarkIds == null || bookmarkIds.isEmpty()) {
            throw new RuntimeException("请选择要分享的书签");
        }

        // 验证书签是否属于当前用户
        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .in("id", bookmarkIds)
                        .eq("user_id", currentUser.getId())
                        .eq("status", 1));

        if (bookmarks.isEmpty()) {
            throw new RuntimeException("未找到有效的书签");
        }

        // 获取有效的书签ID列表
        List<Long> validIds = bookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());
        String idsString = validIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 创建新分享
        SharedBookmarks share = new SharedBookmarks();
        share.setUserId(currentUser.getId());
        share.setTitle(title != null && !title.isEmpty() ? title : "分享的书签");
        share.setBookmarkIds(idsString);
        share.setShareCode(RandomUtil.randomString(8)); // 8位随机码
        share.setPassword(password != null && !password.isEmpty() ? passwordEncoder.encode(password) : null);
        share.setExpireTime(expireDays != null ? LocalDateTime.now().plusDays(expireDays) : null);
        share.setViewCount(0);
        share.setStatus(1);

        sharedBookmarksMapper.insert(share);
        return share;
    }

    /**
     * 获取批量分享内容（公开访问）
     */
    public Map<String, Object> getShareContent(String shareCode, String password) {
        SharedBookmarks share = sharedBookmarksMapper.selectOne(
                new QueryWrapper<SharedBookmarks>()
                        .eq("share_code", shareCode)
                        .eq("status", 1));

        if (share == null) {
            throw new RuntimeException("分享不存在或已过期");
        }

        // 检查是否过期
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("分享链接已过期");
        }

        // 检查密码
        if (share.getPassword() != null) {
            if (password == null || !passwordEncoder.matches(password, share.getPassword())) {
                throw new RuntimeException("密码错误");
            }
        }

        // 更新访问次数
        share.setViewCount(share.getViewCount() + 1);
        sharedBookmarksMapper.updateById(share);

        // 解析书签ID列表
        List<Long> bookmarkIds = Arrays.stream(share.getBookmarkIds().split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 获取书签列表
        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .in("id", bookmarkIds)
                        .eq("status", 1)
                        .orderByDesc("create_time"));

        Map<String, Object> result = new HashMap<>();
        result.put("title", share.getTitle());
        result.put("bookmarks", bookmarks);
        result.put("viewCount", share.getViewCount());
        result.put("createTime", share.getCreateTime());

        return result;
    }

    /**
     * 取消分享
     */
    public void cancelShare(Long shareId) {
        User currentUser = userService.getCurrentUser();
        SharedBookmarks share = sharedBookmarksMapper.selectById(shareId);

        if (share == null || !share.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("分享不存在");
        }

        share.setStatus(0);
        sharedBookmarksMapper.updateById(share);
    }

    /**
     * 获取我的批量分享列表
     */
    public List<SharedBookmarks> myShares() {
        User currentUser = userService.getCurrentUser();
        return sharedBookmarksMapper.selectList(
                new QueryWrapper<SharedBookmarks>()
                        .eq("user_id", currentUser.getId())
                        .eq("status", 1)
                        .orderByDesc("create_time"));
    }

    /**
     * 检查是否需要密码
     */
    public boolean needPassword(String shareCode) {
        SharedBookmarks share = sharedBookmarksMapper.selectOne(
                new QueryWrapper<SharedBookmarks>()
                        .eq("share_code", shareCode)
                        .eq("status", 1));
        return share != null && share.getPassword() != null;
    }
}
