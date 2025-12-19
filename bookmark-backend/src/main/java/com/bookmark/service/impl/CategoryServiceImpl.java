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
import com.bookmark.service.CategoryService;
import com.bookmark.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public Category createCategory(CategoryRequest request) {
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
        return category;
    }

    @Override
    public Category updateCategory(Long id, CategoryRequest request) {
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
        return category;
    }

    @Override
    public void deleteCategory(Long id) {
        User currentUser = userService.getCurrentUser();
        Category category = categoryMapper.selectById(id);

        if (category == null || !category.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("分类不存在或无权限");
        }

        // 将该分类下的书签设置为未分类（categoryId = null）
        bookmarkMapper.update(null, new UpdateWrapper<Bookmark>()
                .eq("category_id", id)
                .eq("user_id", currentUser.getId())
                .set("category_id", null));

        categoryMapper.deleteById(id);
    }

    @Override
    public List<Category> getCategoryList() {
        User currentUser = userService.getCurrentUser();
        return categoryMapper.selectList(new QueryWrapper<Category>()
                .eq("user_id", currentUser.getId())
                .orderByAsc("sort_order", "create_time"));
    }
}
