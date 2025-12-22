package com.bookmark.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.entity.*;
import com.bookmark.mapper.*;
import com.bookmark.service.ActivationCodeService;
import com.bookmark.service.AdminLogService;
import com.bookmark.service.LoginHistoryService;
import com.bookmark.service.TokenService;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理控制器
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private BookmarkMapper bookmarkMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ActivationCodeMapper activationCodeMapper;

    @Autowired
    private ActivationCodeService activationCodeService;

    @Autowired
    private AdminLogService adminLogService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginHistoryService loginHistoryService;

    /**
     * 获取用户列表
     */
    @GetMapping
    public Result<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer loginType,
            @RequestParam(required = false) Integer isAdmin) {

        // 验证管理员权限
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getNickname, keyword));
        }

        // 状态筛选（不传则显示所有用户，包括禁用的）
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        // 登录方式筛选
        if (loginType != null) {
            wrapper.eq(User::getLoginType, loginType);
        }

        // 管理员筛选
        if (isAdmin != null) {
            wrapper.eq(User::getIsAdmin, isAdmin);
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> pageResult = userMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());

        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 隐藏密码
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);

        // 统计数据
        result.put("bookmarkCount", bookmarkMapper.selectCount(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, id)
                        .eq(Bookmark::getStatus, 1)));
        result.put("categoryCount", categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getUserId, id)));
        result.put("tagCount", tagMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getUserId, id)));

        // 有效激活数
        List<UserActivation> activations = activationCodeService.getUserActivations(id);
        long activeCount = activations.stream().filter(a -> a.getStatus() == 1).count();
        result.put("activeActivations", activeCount);

        return Result.success(result);
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/{id}/status")
    public Result<String> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("无效的状态值");
        }

        // 获取原用户状态
        User targetUser = userMapper.selectById(id);
        if (targetUser == null) {
            return Result.error("用户不存在");
        }
        Integer oldStatus = targetUser.getStatus();

        // 使用 LambdaUpdateWrapper 明确更新 status 字段
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, id)
                .set(User::getStatus, status);
        int rows = userMapper.update(null, updateWrapper);

        if (rows == 0) {
            return Result.error("更新失败");
        }

        // 记录日志
        String actionType = status == 1 ? "启用用户" : "禁用用户";
        adminLogService.log(currentUser.getId(), actionType, "user", id,
                "status:" + oldStatus, "status:" + status);

        // 如果是禁用用户，注销其所有 Token
        if (status == 0) {
            tokenService.revokeAllUserTokens(id);
        }

        return Result.success(status == 1 ? "用户已启用" : "用户已禁用");
    }

    /**
     * 设置管理员权限
     */
    @PutMapping("/{id}/admin")
    public Result<String> setAdmin(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Integer isAdmin = body.get("isAdmin");
        if (isAdmin == null || (isAdmin != 0 && isAdmin != 1)) {
            return Result.error("无效的权限值");
        }

        User user = new User();
        user.setId(id);
        user.setIsAdmin(isAdmin);
        userMapper.updateById(user);

        // 记录日志
        String actionType = isAdmin == 1 ? "设置管理员" : "取消管理员";
        adminLogService.log(currentUser.getId(), actionType, "user", id);

        return Result.success(isAdmin == 1 ? "已设为管理员" : "已取消管理员权限");
    }

    /**
     * 获取用户书签列表
     */
    @GetMapping("/{id}/bookmarks")
    public Result<Map<String, Object>> getUserBookmarks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Page<Bookmark> pageResult = bookmarkMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, id)
                        .eq(Bookmark::getStatus, 1)
                        .orderByDesc(Bookmark::getCreateTime));

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());

        return Result.success(result);
    }

    /**
     * 获取用户激活记录（包含激活码）
     */
    @GetMapping("/{id}/activations")
    public Result<List<Map<String, Object>>> getUserActivations(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        List<UserActivation> activations = activationCodeService.getUserActivations(id);

        // 转换为包含激活码字符串的 Map 列表
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (UserActivation activation : activations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", activation.getId());
            item.put("extraBookmarks", activation.getExtraBookmarks());
            item.put("extraCategories", activation.getExtraCategories());
            item.put("expireTime", activation.getExpireTime());
            item.put("status", activation.getStatus());
            item.put("createTime", activation.getCreateTime());

            // 获取激活码字符串
            if (activation.getCodeId() != null) {
                ActivationCode code = activationCodeMapper.selectById(activation.getCodeId());
                item.put("code", code != null ? code.getCode() : "N/A");
            } else {
                item.put("code", "N/A");
            }
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取用户登录历史
     */
    @GetMapping("/{id}/login-history")
    public Result<Map<String, Object>> getLoginHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Map<String, Object> result = loginHistoryService.getLoginHistory(id, page, size);
        return Result.success(result);
    }
}
