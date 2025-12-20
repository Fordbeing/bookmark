package com.bookmark.service;

import com.bookmark.dto.request.CategoryRequest;
import com.bookmark.entity.Category;

import java.util.List;

public interface CategoryService {
    Category createCategory(CategoryRequest request);

    Category updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id, boolean deleteBookmarks);

    List<Category> getCategoryList();

    void updateCategorySortOrder(List<Long> categoryIds);
}
