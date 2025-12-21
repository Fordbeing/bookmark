package com.bookmark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.dto.request.LoginRequest;
import com.bookmark.dto.request.RegisterRequest;
import com.bookmark.dto.response.LoginResponse;
import com.bookmark.entity.User;
import com.bookmark.entity.UserSettings;
import com.bookmark.mapper.UserMapper;
import com.bookmark.mapper.UserSettingsMapper;
import com.bookmark.security.JwtTokenProvider;
import com.bookmark.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserSettingsMapper userSettingsMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginLockService loginLockService;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private SystemConfigService systemConfigService;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        try {
            // Check if email exists
            User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("email", request.getEmail()));
            if (existingUser != null) {
                throw new RuntimeException("邮箱已被注册");
            }

            // Check if username exists
            existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", request.getUsername()));
            if (existingUser != null) {
                throw new RuntimeException("用户名已被使用");
            }

            // 强密码校验（仅对新用户）
            String passwordError = passwordValidator.validate(request.getPassword());
            if (passwordError != null) {
                throw new RuntimeException(passwordError);
            }

            // Create user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setStatus(1);
            userMapper.insert(user);

            // Create default settings
            UserSettings settings = defaultSettings(user);
            userSettingsMapper.insert(settings);

            return getLoginResponse(user);
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage(), e);
            throw new RuntimeException("注册失败，请稍后重试");
        }
    }

    private UserSettings defaultSettings(User user) {
        UserSettings settings = new UserSettings();
        settings.setUserId(user.getId());
        settings.setTheme("light");
        settings.setPrimaryColor("#2563eb");
        settings.setSecondaryColor("#1e40af");
        settings.setAccentColor("#f59e0b");
        settings.setBackgroundColor("#ffffff");
        settings.setSidebarColorFrom("#2563eb");
        settings.setSidebarColorTo("#1e3a8a");
        settings.setDisplayMode("card");
        settings.setAutoOpenNewTab(1);
        settings.setShowStats(1);
        int categoryLimit = Integer.parseInt(systemConfigService.getConfig("default_category_limit"));
        int bookMarkLimit = Integer.parseInt(systemConfigService.getConfig("default_bookmark_limit"));
        settings.setDefaultCategoryLimit(categoryLimit); // 分类上限
        settings.setDefaultBookmarkLimit(bookMarkLimit); // 书签上限
        return settings;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();

        // 检查账户是否被锁定
        long lockSeconds = loginLockService.isLocked(email);
        if (lockSeconds > 0) {
            long minutes = lockSeconds / 60;
            long seconds = lockSeconds % 60;
            String timeStr = minutes > 0 ? minutes + "分钟" + (seconds > 0 ? seconds + "秒" : "") : seconds + "秒";
            throw new RuntimeException("账户已被锁定，请" + timeStr + "后重试");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user
            User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", request.getEmail()));

            // 检查用户状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new RuntimeException("账户已被禁用，请联系管理员");
            }

            // 登录成功，清除失败记录
            loginLockService.clearLoginFailure(email);

            // Update last login time
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            // 生成双 Token
            LoginResponse response = getLoginResponse(user);

            log.info("用户登录成功: userId={}, email={}", user.getId(), user.getEmail());
            return response;
        } catch (RuntimeException e) {
            // 登录失败，记录失败次数
            if (!e.getMessage().contains("已被锁定") && !e.getMessage().contains("已被禁用")) {
                int remaining = loginLockService.getRemainingAttempts(email) - 1;
                loginLockService.recordLoginFailure(email);
                if (remaining > 0) {
                    throw new RuntimeException("登录失败，还剩余 " + remaining + " 次尝试机会");
                } else {
                    int lockMinutes = loginLockService.getLockDurationMinutes();
                    throw new RuntimeException("登录失败次数过多，账户已被锁定 " + lockMinutes + " 分钟");
                }
            }
            throw e;
        } catch (Exception e) {
            // 登录失败，记录失败次数
            int remaining = loginLockService.getRemainingAttempts(email) - 1;
            loginLockService.recordLoginFailure(email);
            if (remaining > 0) {
                log.error("用户登录失败: {}", e.getMessage(), e);
                throw new RuntimeException("登录失败，还剩余 " + remaining + " 次尝试机会");
            } else {
                int lockMinutes = loginLockService.getLockDurationMinutes();
                throw new RuntimeException("登录失败次数过多，账户已被锁定 " + lockMinutes + " 分钟");
            }
        }
    }

    private LoginResponse getLoginResponse(User user) {
        String accessTokenId = tokenService.generateTokenId();
        String refreshTokenId = tokenService.generateTokenId();

        String accessToken = tokenProvider.generateAccessToken(user.getId(), accessTokenId);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), refreshTokenId);

        // 存储到 Redis
        tokenService.storeAccessToken(user.getId(), accessTokenId);
        tokenService.storeRefreshToken(user.getId(), refreshTokenId);

        // Build response
        return getLoginResponse(user, accessToken, refreshToken);
    }

    private LoginResponse getLoginResponse(User user, String accessToken, String refreshToken) {
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setIsAdmin(user.getIsAdmin());
        response.setToken(accessToken); // 兼容旧版前端
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(tokenProvider.getAccessTokenExpiration() / 1000);
        return response;
    }

    @Override
    public User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                throw new RuntimeException("未找到认证信息，请重新登录");
            }

            String identifier = authentication.getName();

            if (identifier == null || "anonymousUser".equals(identifier)) {
                throw new RuntimeException("用户未登录或会话已过期，请重新登录");
            }

            User user = null;

            // 首先尝试按 ID 查询（JWT token 中存储的是用户 ID）
            try {
                Long userId = Long.parseLong(identifier);
                user = userMapper.selectById(userId);
            } catch (NumberFormatException e) {
                // 不是数字，按邮箱查询
            }

            // 如果按 ID 没找到，尝试按邮箱查询
            if (user == null) {
                user = userMapper.selectOne(new QueryWrapper<User>().eq("email", identifier));
            }

            if (user == null) {
                throw new RuntimeException("用户不存在，请重新登录");
            }

            return user;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取当前用户失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取用户信息失败");
        }
    }
}
