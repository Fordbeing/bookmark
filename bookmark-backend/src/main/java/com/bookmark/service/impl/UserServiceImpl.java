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
import com.bookmark.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
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

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);

        // Create default settings
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
        userSettingsMapper.insert(settings);

        // Generate token
        String token = tokenProvider.generateToken(user.getEmail());

        // Build response
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setToken(token);

        return response;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get user
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", request.getEmail()));

        // Update last login time
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // Generate token
        String token = tokenProvider.generateToken(user.getEmail());

        // Build response
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setToken(token);

        return response;
    }

    @Override
    public User getCurrentUser() {
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
    }
}
