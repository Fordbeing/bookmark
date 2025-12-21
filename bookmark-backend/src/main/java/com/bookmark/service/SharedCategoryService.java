package com.bookmark.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.Category;
import com.bookmark.entity.SharedCategory;
import com.bookmark.entity.User;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.mapper.CategoryMapper;
import com.bookmark.mapper.SharedCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分享服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharedCategoryService {

    private final SharedCategoryMapper sharedCategoryMapper;
    private final BookmarkMapper bookmarkMapper;
    private final CategoryMapper categoryMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建分享
     */
    public SharedCategory createShare(Long categoryId, String password, Integer expireDays) {
        User currentUser = userService.getCurrentUser();

        // 检查分类是否属于当前用户
        Category category = categoryMapper.selectOne(
                new QueryWrapper<Category>()
                        .eq("id", categoryId)
                        .eq("user_id", currentUser.getId()));
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 检查是否已有分享
        SharedCategory existing = sharedCategoryMapper.selectOne(
                new QueryWrapper<SharedCategory>()
                        .eq("user_id", currentUser.getId())
                        .eq("category_id", categoryId)
                        .eq("status", 1));
        if (existing != null) {
            // 返回已有的分享
            return existing;
        }

        // 创建新分享
        SharedCategory share = new SharedCategory();
        share.setUserId(currentUser.getId());
        share.setCategoryId(categoryId);
        share.setShareCode(RandomUtil.randomString(8)); // 8位随机码
        share.setPassword(password != null && !password.isEmpty() ? passwordEncoder.encode(password) : null);
        share.setExpireTime(expireDays != null ? LocalDateTime.now().plusDays(expireDays) : null);
        share.setViewCount(0);
        share.setStatus(1);

        sharedCategoryMapper.insert(share);
        return share;
    }

    /**
     * 获取分享内容（公开访问）
     */
    public Map<String, Object> getShareContent(String shareCode, String password) {
        SharedCategory share = sharedCategoryMapper.selectOne(
                new QueryWrapper<SharedCategory>()
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
        sharedCategoryMapper.updateById(share);

        // 获取分类信息
        Category category = categoryMapper.selectById(share.getCategoryId());

        // 获取分类下的书签
        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new QueryWrapper<Bookmark>()
                        .eq("category_id", share.getCategoryId())
                        .eq("status", 1)
                        .orderByDesc("create_time"));

        Map<String, Object> result = new HashMap<>();
        result.put("categoryName", category != null ? category.getName() : "未知分类");
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
        SharedCategory share = sharedCategoryMapper.selectById(shareId);

        if (share == null || !share.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("分享不存在");
        }

        share.setStatus(0);
        sharedCategoryMapper.updateById(share);
    }

    /**
     * 获取我的分享列表
     */
    public List<SharedCategory> myShares() {
        User currentUser = userService.getCurrentUser();
        return sharedCategoryMapper.selectList(
                new QueryWrapper<SharedCategory>()
                        .eq("user_id", currentUser.getId())
                        .eq("status", 1)
                        .orderByDesc("create_time"));
    }

    /**
     * 检查是否需要密码
     */
    public boolean needPassword(String shareCode) {
        SharedCategory share = sharedCategoryMapper.selectOne(
                new QueryWrapper<SharedCategory>()
                        .eq("share_code", shareCode)
                        .eq("status", 1));
        return share != null && share.getPassword() != null;
    }
}
