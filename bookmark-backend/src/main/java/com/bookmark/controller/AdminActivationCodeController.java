package com.bookmark.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmark.entity.ActivationCode;
import com.bookmark.entity.User;
import com.bookmark.entity.UserActivation;
import com.bookmark.mapper.ActivationCodeMapper;
import com.bookmark.mapper.UserActivationMapper;
import com.bookmark.mapper.UserMapper;
import com.bookmark.service.ActivationCodeService;
import com.bookmark.service.AdminLogService;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员激活码管理控制器
 */
@RestController
@RequestMapping("/admin/activation-codes")
public class AdminActivationCodeController {

    @Autowired
    private ActivationCodeMapper activationCodeMapper;

    @Autowired
    private UserActivationMapper userActivationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivationCodeService activationCodeService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminLogService adminLogService;

    /**
     * 获取激活码列表
     */
    @GetMapping
    public Result<Map<String, Object>> getCodeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        LambdaQueryWrapper<ActivationCode> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(ActivationCode::getCode, keyword);
        }

        if (status != null) {
            wrapper.eq(ActivationCode::getStatus, status);
        }

        wrapper.orderByDesc(ActivationCode::getCreateTime);

        Page<ActivationCode> pageResult = activationCodeMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());

        return Result.success(result);
    }

    /**
     * 创建单个激活码
     */
    @PostMapping
    public Result<ActivationCode> createCode(@RequestBody Map<String, Integer> request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Integer extraBookmarks = request.getOrDefault("extraBookmarks", 100);
        Integer extraCategories = request.getOrDefault("extraCategories", 10);
        Integer expireDays = request.getOrDefault("expireDays", 30);
        Integer maxUses = request.getOrDefault("maxUses", 1);

        ActivationCode code = activationCodeService.createCode(extraBookmarks, extraCategories, expireDays, maxUses);

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "创建激活码", "激活码", code.getId(), null, code.getCode());

        return Result.success("激活码创建成功", code);
    }

    /**
     * 批量创建激活码
     */
    @PostMapping("/batch")
    public Result<List<ActivationCode>> batchCreateCodes(@RequestBody Map<String, Integer> request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Integer count = request.getOrDefault("count", 10);
        Integer extraBookmarks = request.getOrDefault("extraBookmarks", 100);
        Integer extraCategories = request.getOrDefault("extraCategories", 10);
        Integer expireDays = request.getOrDefault("expireDays", 30);
        Integer maxUses = request.getOrDefault("maxUses", 1);

        if (count > 100) {
            return Result.error("单次最多创建100个激活码");
        }

        List<ActivationCode> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ActivationCode code = activationCodeService.createCode(extraBookmarks, extraCategories, expireDays,
                    maxUses);
            codes.add(code);
        }

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "批量创建激活码", "激活码", null, null, "创建" + count + "个激活码");

        return Result.success("成功创建 " + count + " 个激活码", codes);
    }

    /**
     * 修改激活码状态
     */
    @PutMapping("/{id}")
    public Result<String> updateCode(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        ActivationCode code = activationCodeMapper.selectById(id);
        if (code == null) {
            return Result.error("激活码不存在");
        }

        if (body.containsKey("status")) {
            code.setStatus((Integer) body.get("status"));
        }

        activationCodeMapper.updateById(code);

        // 记录操作日志
        String actionType = (Integer) body.get("status") == 1 ? "启用激活码" : "禁用激活码";
        adminLogService.log(currentUser.getId(), actionType, "激活码", id, null, code.getCode());

        return Result.success("激活码已更新");
    }

    /**
     * 删除激活码
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteCode(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        ActivationCode code = activationCodeMapper.selectById(id);
        String codeValue = code != null ? code.getCode() : String.valueOf(id);

        activationCodeMapper.deleteById(id);

        // 记录操作日志
        adminLogService.log(currentUser.getId(), "删除激活码", "激活码", id, codeValue, null);

        return Result.success("激活码已删除");
    }

    /**
     * 获取激活码使用记录
     */
    @GetMapping("/{id}/usage")
    public Result<List<Map<String, Object>>> getCodeUsage(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        List<UserActivation> activations = userActivationMapper.selectList(
                new LambdaQueryWrapper<UserActivation>().eq(UserActivation::getCodeId, id));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserActivation activation : activations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", activation.getId());
            item.put("userId", activation.getUserId());

            User user = userMapper.selectById(activation.getUserId());
            item.put("username", user != null ? user.getUsername() : "未知用户");

            item.put("createTime", activation.getCreateTime());
            item.put("expireTime", activation.getExpireTime());
            item.put("status", activation.getStatus());

            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取所有用户激活记录
     */
    @GetMapping("/user-activations")
    public Result<Map<String, Object>> getUserActivations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        Page<UserActivation> pageResult = userActivationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<UserActivation>().orderByDesc(UserActivation::getCreateTime));

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());

        return Result.success(result);
    }
}
