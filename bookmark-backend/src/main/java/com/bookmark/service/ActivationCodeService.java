package com.bookmark.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.ActivationCode;
import com.bookmark.entity.User;
import com.bookmark.entity.UserActivation;
import com.bookmark.mapper.ActivationCodeMapper;
import com.bookmark.mapper.UserActivationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 激活码服务
 */
@Service
public class ActivationCodeService {

    @Autowired
    private ActivationCodeMapper activationCodeMapper;

    @Autowired
    private UserActivationMapper userActivationMapper;

    @Autowired
    private UserService userService;

    private static final int BASE_BOOKMARK_LIMIT = 200;
    private static final int BASE_CATEGORY_LIMIT = 20;

    /**
     * 创建激活码（仅管理员）
     */
    public ActivationCode createCode(Integer extraBookmarks, Integer extraCategories,
            Integer expireDays, Integer maxUses) {
        User currentUser = userService.getCurrentUser();

        // 检查是否管理员
        if (currentUser.getIsAdmin() == null || currentUser.getIsAdmin() != 1) {
            throw new RuntimeException("无权限创建激活码");
        }

        ActivationCode code = new ActivationCode();
        code.setCode(generateCode());
        code.setExtraBookmarks(extraBookmarks != null ? extraBookmarks : 10);
        code.setExtraCategories(extraCategories != null ? extraCategories : 3);
        code.setExpireDays(expireDays != null ? expireDays : 30);
        code.setMaxUses(maxUses != null ? maxUses : 1);
        code.setUsedCount(0);
        code.setCreatedBy(currentUser.getId());
        code.setStatus(1);

        activationCodeMapper.insert(code);
        return code;
    }

    /**
     * 获取激活码列表（仅管理员）
     */
    public List<ActivationCode> getCodeList() {
        User currentUser = userService.getCurrentUser();

        if (currentUser.getIsAdmin() == null || currentUser.getIsAdmin() != 1) {
            throw new RuntimeException("无权限查看激活码");
        }

        return activationCodeMapper.selectList(
                new QueryWrapper<ActivationCode>()
                        .eq("created_by", currentUser.getId())
                        .orderByDesc("create_time"));
    }

    /**
     * 兑换激活码
     */
    public UserActivation redeemCode(String codeStr) {
        User currentUser = userService.getCurrentUser();

        // 查找激活码
        ActivationCode code = activationCodeMapper.selectOne(
                new QueryWrapper<ActivationCode>()
                        .eq("code", codeStr)
                        .eq("status", 1));

        if (code == null) {
            throw new RuntimeException("激活码无效或已禁用");
        }

        // 检查使用次数
        if (code.getUsedCount() >= code.getMaxUses()) {
            throw new RuntimeException("激活码已达最大使用次数");
        }

        // 检查是否已使用过该激活码
        UserActivation existing = userActivationMapper.selectOne(
                new QueryWrapper<UserActivation>()
                        .eq("user_id", currentUser.getId())
                        .eq("code_id", code.getId()));

        if (existing != null) {
            throw new RuntimeException("您已使用过该激活码");
        }

        // 创建激活记录
        UserActivation activation = new UserActivation();
        activation.setUserId(currentUser.getId());
        activation.setCodeId(code.getId());
        activation.setExtraBookmarks(code.getExtraBookmarks());
        activation.setExtraCategories(code.getExtraCategories());
        activation.setExpireTime(LocalDateTime.now().plusDays(code.getExpireDays()));
        activation.setStatus(1);

        userActivationMapper.insert(activation);

        // 更新使用次数
        code.setUsedCount(code.getUsedCount() + 1);
        activationCodeMapper.updateById(code);

        return activation;
    }

    /**
     * 获取用户当前书签限制
     */
    public int getUserBookmarkLimit(Long userId) {
        User user = userService.getCurrentUser();

        // 管理员无限制
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            return Integer.MAX_VALUE;
        }

        // Redis定时任务会自动更新过期状态，这里直接查询即可
        Integer extra = userActivationMapper.getActiveExtraBookmarks(userId);
        return BASE_BOOKMARK_LIMIT + (extra != null ? extra : 0);
    }

    /**
     * 获取用户当前分类限制
     */
    public int getUserCategoryLimit(Long userId) {
        User user = userService.getCurrentUser();

        // 管理员无限制
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            return Integer.MAX_VALUE;
        }

        // Redis定时任务会自动更新过期状态，这里直接查询即可
        Integer extra = userActivationMapper.getActiveExtraCategories(userId);
        return BASE_CATEGORY_LIMIT + (extra != null ? extra : 0);
    }

    /**
     * 获取用户激活记录
     */
    public List<UserActivation> getUserActivations(Long userId) {
        return userActivationMapper.selectList(
                new QueryWrapper<UserActivation>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time"));
    }

    /**
     * 生成随机激活码
     */
    private String generateCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
