package com.bookmark.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 密码校验服务
 * 实现强密码校验规则
 */
@Slf4j
@Service
public class PasswordValidator {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 校验密码强度
     * 
     * @param password 密码
     * @return 校验结果，null表示通过，否则返回错误信息
     */
    public String validate(String password) {
        // 检查是否启用强密码校验
        if (!isStrongPasswordRequired()) {
            return null; // 未启用，直接通过
        }

        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }

        // 长度检查：至少8位
        if (password.length() < 8) {
            return "密码长度至少8位";
        }

        // 检查是否包含小写字母
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含小写字母";
        }

        // 检查是否包含大写字母
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含大写字母";
        }

        // 检查是否包含数字
        if (!password.matches(".*\\d.*")) {
            return "密码必须包含数字";
        }

        // 检查是否包含特殊字符
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>\\[\\]\\-_+=~`/\\\\].*")) {
            return "密码必须包含特殊字符（如 !@#$%^&* 等）";
        }

        return null; // 校验通过
    }

    /**
     * 检查是否启用强密码要求
     */
    public boolean isStrongPasswordRequired() {
        return systemConfigService.getConfigBoolean("require_strong_password", false);
    }

    /**
     * 获取密码要求描述
     */
    public String getPasswordRequirements() {
        if (!isStrongPasswordRequired()) {
            return "密码长度不限";
        }
        return "密码至少8位，需包含大小写字母、数字和特殊字符";
    }
}
