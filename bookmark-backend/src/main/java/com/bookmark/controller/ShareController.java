package com.bookmark.controller;

import com.bookmark.entity.SharedCategory;
import com.bookmark.service.SharedCategoryService;
import com.bookmark.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分享管理控制器（需要登录）
 */
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final SharedCategoryService sharedCategoryService;

    /**
     * 创建分享
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createShare(@RequestBody Map<String, Object> request) {
        Long categoryId = Long.parseLong(request.get("categoryId").toString());
        String password = (String) request.get("password");
        Integer expireDays = request.get("expireDays") != null ? Integer.parseInt(request.get("expireDays").toString())
                : null;

        SharedCategory share = sharedCategoryService.createShare(categoryId, password, expireDays);

        Map<String, Object> response = new HashMap<>();
        response.put("shareCode", share.getShareCode());
        response.put("shareUrl", "/public/share/" + share.getShareCode());
        response.put("hasPassword", share.getPassword() != null);
        response.put("expireTime", share.getExpireTime());

        return Result.success("分享创建成功", response);
    }

    /**
     * 取消分享
     */
    @DeleteMapping("/{id}")
    public Result<Void> cancelShare(@PathVariable Long id) {
        sharedCategoryService.cancelShare(id);
        return Result.success("分享已取消", null);
    }

    /**
     * 获取我的分享列表
     */
    @GetMapping("/my-shares")
    public Result<List<SharedCategory>> myShares() {
        List<SharedCategory> shares = sharedCategoryService.myShares();
        return Result.success(shares);
    }
}
