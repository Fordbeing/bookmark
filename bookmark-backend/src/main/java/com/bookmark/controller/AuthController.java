package com.bookmark.controller;

import com.bookmark.dto.request.BindPhoneRequest;
import com.bookmark.dto.request.LoginRequest;
import com.bookmark.dto.request.RegisterRequest;
import com.bookmark.dto.request.WxLoginRequest;
import com.bookmark.dto.response.LoginResponse;
import com.bookmark.entity.User;
import com.bookmark.service.UserService;
import com.bookmark.service.WxAuthService;
import com.bookmark.util.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private WxAuthService wxAuthService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = userService.register(request);
        return Result.success("注册成功", response);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
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

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }
}
