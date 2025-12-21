package com.bookmark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bookmark.dto.request.CategoryRequest;
import com.bookmark.entity.Bookmark;
import com.bookmark.entity.Category;
import com.bookmark.entity.User;
import com.bookmark.mapper.BookmarkMapper;
import com.bookmark.mapper.CategoryMapper;
import com.bookmark.service.ActivationCodeService;
import com.bookmark.service.CategoryCacheService;
import com.bookmark.service.CategoryService;
import com.bookmark.service.SearchService;
import com.bookmark.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BookmarkMapper bookmarkMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ActivationCodeService activationCodeService;

    @Autowired
    private CategoryCacheService categoryCacheService;

    @Autowired
    private SearchService searchService;

    @Override
    public Category createCategory(CategoryRequest request) {
        try {
            User currentUser = userService.getCurrentUser();

            // 检查分类数量限制（使用动态限制）
            int categoryLimit = activationCodeService.getUserCategoryLimit(currentUser.getId());
            Long categoryCount = categoryMapper.selectCount(
                    new QueryWrapper<Category>()
                            .eq("user_id", currentUser.getId()));
            if (categoryCount >= categoryLimit) {
                throw new RuntimeException("分类数量已达上限（" + categoryLimit + "个），请删除部分分类或使用激活码增加额度");
            }

            // Check if category name exists
            Category existing = categoryMapper.selectOne(new QueryWrapper<Category>()
                    .eq("user_id", currentUser.getId())
                    .eq("name", request.getName()));

            if (existing != null) {
                throw new RuntimeException("分类名称已存在");
            }

            Category category = new Category();
            category.setUserId(currentUser.getId());
            category.setName(request.getName());
            category.setColor(request.getColor());
            category.setIcon(request.getIcon());
            category.setSortOrder(0);

            categoryMapper.insert(category);

            // 刷新缓存
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }

            return category;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建分类失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建分类失败");
        }
    }

    @Override
    public Category updateCategory(Long id, CategoryRequest request) {
        try {
            User currentUser = userService.getCurrentUser();
            Category category = categoryMapper.selectById(id);

            if (category == null || !category.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("分类不存在或无权限");
            }

            if (request.getName() != null)
                category.setName(request.getName());
            if (request.getColor() != null)
                category.setColor(request.getColor());
            if (request.getIcon() != null)
                category.setIcon(request.getIcon());

            categoryMapper.updateById(category);

            // 刷新缓存
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }

            return category;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新分类失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新分类失败");
        }
    }

    @Override
    public void deleteCategory(Long id, boolean deleteBookmarks) {
        try {
            User currentUser = userService.getCurrentUser();
            Category category = categoryMapper.selectById(id);

            if (category == null || !category.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("分类不存在或无权限");
            }

            if (deleteBookmarks) {
                // 删除该分类下的所有书签（包括ES）
                List<Bookmark> bookmarks = bookmarkMapper.selectList(
                        new QueryWrapper<Bookmark>()
                                .eq("category_id", id)
                                .eq("user_id", currentUser.getId()));

                for (Bookmark bookmark : bookmarks) {
                    try {
                        searchService.deleteBookmark(bookmark.getId());
                    } catch (Exception e) {
                        log.error("从 Elasticsearch 删除书签失败: {}", e.getMessage(), e);
                    }
                }

                bookmarkMapper.delete(new QueryWrapper<Bookmark>()
                        .eq("category_id", id)
                        .eq("user_id", currentUser.getId()));
            } else {
                // 将书签移至未分类
                bookmarkMapper.update(null, new UpdateWrapper<Bookmark>()
                        .eq("category_id", id)
                        .eq("user_id", currentUser.getId())
                        .set("category_id", null));
            }

            categoryMapper.deleteById(id);

            // 刷新缓存
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除分类失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除分类失败");
        }
    }

    @Override
    public List<Category> getCategoryList() {
        try {
            User currentUser = userService.getCurrentUser();
            // 使用缓存获取分类列表（已包含书签数量）
            return categoryCacheService.getUserCategories(currentUser.getId());
        } catch (Exception e) {
            log.error("获取分类列表失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateCategorySortOrder(List<Long> categoryIds) {
        try {
            User currentUser = userService.getCurrentUser();

            // 批量更新sortOrder
            for (int i = 0; i < categoryIds.size(); i++) {
                Long categoryId = categoryIds.get(i);
                Category category = categoryMapper.selectById(categoryId);

                // 确保分类属于当前用户
                if (category != null && category.getUserId().equals(currentUser.getId())) {
                    category.setSortOrder(i);
                    categoryMapper.updateById(category);
                }
            }

            // 刷新 Redis 缓存
            try {
                categoryCacheService.refreshUserCategoriesCache(currentUser.getId());
            } catch (Exception e) {
                log.error("刷新分类缓存失败: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("更新分类排序失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新分类排序失败");
        }
    }
}
