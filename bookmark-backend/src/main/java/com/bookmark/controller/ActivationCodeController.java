package com.bookmark.controller;

import com.bookmark.entity.ActivationCode;
import com.bookmark.entity.User;
import com.bookmark.entity.UserActivation;
import com.bookmark.service.ActivationCodeService;
import com.bookmark.service.ActivationExpiryTask;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 激活码控制器
 */
@RestController
@RequestMapping("/activation-codes")
public class ActivationCodeController {

    @Autowired
    private ActivationCodeService activationCodeService;

    @Autowired
    private UserService userService;

    @Autowired
    private ActivationExpiryTask activationExpiryTask;

    /**
     * 创建激活码（仅管理员）
     */
    @PostMapping
    public Result<ActivationCode> createCode(@RequestBody Map<String, Integer> request) {
        Integer extraBookmarks = request.get("extraBookmarks");
        Integer extraCategories = request.get("extraCategories");
        Integer expireDays = request.get("expireDays");
        Integer maxUses = request.get("maxUses");

        ActivationCode code = activationCodeService.createCode(
                extraBookmarks, extraCategories, expireDays, maxUses);
        return Result.success("激活码创建成功", code);
    }

    /**
     * 获取激活码列表（仅管理员）
     */
    @GetMapping
    public Result<List<ActivationCode>> getCodeList() {
        List<ActivationCode> list = activationCodeService.getCodeList();
        return Result.success(list);
    }

    /**
     * 兑换激活码
     */
    @PostMapping("/redeem")
    public Result<UserActivation> redeemCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error("请输入激活码");
        }

        UserActivation activation = activationCodeService.redeemCode(code);

        // 将激活码加入 Redis 过期队列
        activationExpiryTask.addToExpiryQueue(activation.getId(), activation.getExpireTime());

        return Result.success("激活码兑换成功", activation);
    }

    /**
     * 获取当前用户的限制信息
     */
    @GetMapping("/my-limits")
    public Result<Map<String, Object>> getMyLimits() {
        User currentUser = userService.getCurrentUser();

        Map<String, Object> limits = new HashMap<>();
        limits.put("bookmarkLimit", activationCodeService.getUserBookmarkLimit(currentUser.getId()));
        limits.put("categoryLimit", activationCodeService.getUserCategoryLimit(currentUser.getId()));
        limits.put("isAdmin", currentUser.getIsAdmin() != null && currentUser.getIsAdmin() == 1);
        limits.put("activations", activationCodeService.getUserActivations(currentUser.getId()));

        return Result.success(limits);
    }
}
