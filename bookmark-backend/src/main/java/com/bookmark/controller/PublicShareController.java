package com.bookmark.controller;

import com.bookmark.service.SharedCategoryService;
import com.bookmark.service.SharedBookmarksService;
import com.bookmark.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开分享访问控制器（无需登录）
 */
@RestController
@RequestMapping("/public/share")
@RequiredArgsConstructor
public class PublicShareController {

    private final SharedCategoryService sharedCategoryService;
    private final SharedBookmarksService sharedBookmarksService;

    /**
     * 检查分类分享是否需要密码
     */
    @GetMapping("/{code}/check")
    public Result<Map<String, Object>> checkShare(@PathVariable String code) {
        boolean needPassword = sharedCategoryService.needPassword(code);

        Map<String, Object> response = new HashMap<>();
        response.put("needPassword", needPassword);

        return Result.success(response);
    }

    /**
     * 获取分类分享内容
     */
    @GetMapping("/{code}")
    public Result<Map<String, Object>> getShareContent(
            @PathVariable String code,
            @RequestParam(required = false) String password) {
        try {
            Map<String, Object> content = sharedCategoryService.getShareContent(code, password);
            return Result.success(content);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ========== 批量书签分享相关 ==========

    /**
     * 检查批量分享是否需要密码
     */
    @GetMapping("/batch/{code}/check")
    public Result<Map<String, Object>> checkBatchShare(@PathVariable String code) {
        boolean needPassword = sharedBookmarksService.needPassword(code);

        Map<String, Object> response = new HashMap<>();
        response.put("needPassword", needPassword);

        return Result.success(response);
    }

    /**
     * 获取批量分享内容
     */
    @GetMapping("/batch/{code}")
    public Result<Map<String, Object>> getBatchShareContent(
            @PathVariable String code,
            @RequestParam(required = false) String password) {
        try {
            Map<String, Object> content = sharedBookmarksService.getShareContent(code, password);
            return Result.success(content);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
