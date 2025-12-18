package com.bookmark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.dto.request.CategoryRequest;
import com.bookmark.entity.Category;
import com.bookmark.entity.User;
import com.bookmark.mapper.CategoryMapper;
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
    private UserService userService;

    @Override
    public Category createCategory(CategoryRequest request) {
        User currentUser = userService.getCurrentUser();

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
