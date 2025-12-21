package com.bookmark.controller;

import com.bookmark.dto.request.BindPhoneRequest;
import com.bookmark.dto.request.LoginRequest;
import com.bookmark.dto.request.RegisterRequest;
import com.bookmark.dto.request.WxLoginRequest;
import com.bookmark.dto.response.LoginResponse;
import com.bookmark.entity.User;
import com.bookmark.security.JwtTokenProvider;
import com.bookmark.service.AdminLogService;
import com.bookmark.service.SystemConfigService;
import com.bookmark.service.TokenService;
import com.bookmark.service.UserService;
import com.bookmark.service.WxAuthService;
import com.bookmark.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private WxAuthService wxAuthService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AdminLogService adminLogService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = userService.register(request);
        return Result.success("注册成功", response);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 检查系统是否允许登录
        if (!systemConfigService.isLoginAllowed()) {
            return Result.error("系统当前不允许登录，请联系管理员");
        }

        // 检查维护模式
        if (systemConfigService.isMaintenanceMode()) {
            String msg = systemConfigService.getMaintenanceMessage();
            return Result.error("系统维护中：" + msg);
        }

        LoginResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 管理员登录
     */
    @PostMapping("/admin-login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);

        // 验证是否为管理员
        if (response.getIsAdmin() == null || response.getIsAdmin() != 1) {
            return Result.error("无管理员权限");
        }

        // 记录管理员登录日志
        adminLogService.log(response.getId(), "管理员登录", "用户", response.getId());

        return Result.success("登录成功", response);
    }

    /**
     * 微信小程序登录
     */
    @PostMapping("/wx-login")
    public Result<LoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        LoginResponse response = wxAuthService.wxLogin(request);
        return Result.success("登录成功", response);
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/bind-phone")
    public Result<Boolean> bindPhone(@RequestBody BindPhoneRequest request) {
        User user = userService.getCurrentUser();
        boolean success = wxAuthService.bindPhone(user.getId(), request);
        if (success) {
            return Result.success("绑定成功", true);
        } else {
            return Result.error("该手机号已绑定其他账户，请联系客服合并账户");
        }
    }

    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        // Don't return password
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 使用 Refresh Token 刷新 Access Token
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");

            if (!StringUtils.hasText(refreshToken)) {
                return Result.error("Refresh Token 不能为空");
            }

            // 验证 Refresh Token 签名
            if (!tokenProvider.validateToken(refreshToken)) {
                return Result.error("无效的 Refresh Token");
            }

            // 检查是否为 Refresh Token
            if (!tokenProvider.isRefreshToken(refreshToken)) {
                return Result.error("请使用 Refresh Token");
            }

            Long userId = tokenProvider.getUserIdFromToken(refreshToken);
            String oldTokenId = tokenProvider.getTokenIdFromToken(refreshToken);

            // 验证 Refresh Token 是否在 Redis 中
            if (!tokenService.validateRefreshToken(userId, oldTokenId)) {
                return Result.error("Refresh Token 已过期或被注销");
            }

            // 生成新的 Access Token
            String newAccessTokenId = tokenService.generateTokenId();
            String newAccessToken = tokenProvider.generateAccessToken(userId, newAccessTokenId);
            tokenService.storeAccessToken(userId, newAccessTokenId);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("token", newAccessToken); // 兼容旧版
            result.put("expiresIn", tokenProvider.getAccessTokenExpiration() / 1000);

            log.debug("Token 刷新成功: userId={}", userId);
            return Result.success("刷新成功", result);
        } catch (Exception e) {
            log.error("Token 刷新失败: {}", e.getMessage(), e);
            return Result.error("Token 刷新失败");
        }
    }

    /**
     * 注销登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        try {
            String bearerToken = request.getHeader("Authorization");
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7);

                if (tokenProvider.validateToken(token)) {
                    Long userId = tokenProvider.getUserIdFromToken(token);
                    String tokenId = tokenProvider.getTokenIdFromToken(token);
                    String tokenType = tokenProvider.getTokenTypeFromToken(token);

                    if (tokenId != null && userId != null) {
                        if ("access".equals(tokenType)) {
                            tokenService.revokeAccessToken(userId, tokenId);
                        } else if ("refresh".equals(tokenType)) {
                            tokenService.revokeRefreshToken(userId, tokenId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("注销时清除 Token 失败: {}", e.getMessage());
        }
        return Result.success("退出成功", null);
    }
}
