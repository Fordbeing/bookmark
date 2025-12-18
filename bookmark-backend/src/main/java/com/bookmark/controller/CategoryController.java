package com.bookmark.controller;

import com.bookmark.dto.request.CategoryRequest;
import com.bookmark.entity.Category;
import com.bookmark.service.CategoryService;
import com.bookmark.util.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> getCategoryList() {
        List<Category> categories = categoryService.getCategoryList();
        return Result.success(categories);
    }

    @PostMapping
    public Result<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.createCategory(request);
        return Result.success("分类创建成功", category);
    }

    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        categoryService.updateCategory(id, request);
        return Result.success("分类更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("分类删除成功", null);
    }
}
