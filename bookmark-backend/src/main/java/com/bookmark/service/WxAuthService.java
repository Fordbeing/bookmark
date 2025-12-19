package com.bookmark.service;

import com.bookmark.dto.request.BindPhoneRequest;
import com.bookmark.dto.request.WxLoginRequest;
import com.bookmark.dto.response.LoginResponse;
import com.bookmark.entity.User;
import com.bookmark.mapper.UserMapper;
import com.bookmark.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 微信认证服务
 */
@Service
public class WxAuthService {

    @Value("${wx.miniapp.appid:}")
    private String appId;

    @Value("${wx.miniapp.secret:}")
    private String appSecret;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 微信登录
     * 
     * @param request 包含wx.login()返回的code
     * @return 登录响应(含token)
     */
    public LoginResponse wxLogin(WxLoginRequest request) {
        // 1. 调用微信code2Session接口获取openid
        String openid = getOpenIdFromWx(request.getCode());

        // 2. 根据openid查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));

        // 3. 如果用户不存在,创建新用户
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUsername("wx_" + UUID.randomUUID().toString().substring(0, 8));
            user.setNickname(request.getNickName() != null ? request.getNickName() : "微信用户");
            user.setAvatar(request.getAvatarUrl());
            // 微信登录用户使用占位邮箱（openid的hash）
            user.setEmail("wx_" + openid.hashCode() + "@wx.placeholder");
            user.setPassword(""); // 微信用户无密码
            user.setLoginType(2); // 微信登录
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);
        }

        // 4. 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 5. 生成JWT Token
        String token = jwtTokenProvider.generateToken(String.valueOf(user.getId()));

        // 6. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());

        return response;
    }

    /**
     * 绑定手机号
     * 
     * @param userId  当前用户ID
     * @param request 绑定请求
     * @return 是否成功
     */
    public boolean bindPhone(Long userId, BindPhoneRequest request) {
        // 检查手机号是否已被其他用户绑定
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));

        if (existingUser != null && !existingUser.getId().equals(userId)) {
            // 手机号已绑定其他账户,执行账户合并逻辑
            return mergeAccounts(userId, existingUser.getId());
        }

        // 绑定手机号
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPhone(request.getPhone());
            userMapper.updateById(user);
            return true;
        }
        return false;
    }

    /**
     * 合并账户(将当前用户数据迁移到已存在的用户)
     * 这里简化处理:返回已存在用户的信息,由前端提示用户
     */
    private boolean mergeAccounts(Long currentUserId, Long targetUserId) {
        // 实际项目中需要迁移书签、分类等数据
        // 这里只做标记,让前端处理
        return false;
    }

    /**
     * 调用微信code2Session接口获取openid
     */
    private String getOpenIdFromWx(String code) {
        // 开发环境:如果没有配置appid/secret,使用模拟openid
        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            return "mock_openid_" + code;
        }

        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("openid")) {
                return (String) body.get("openid");
            }
            throw new RuntimeException("微信登录失败: " + (body != null ? body.get("errmsg") : "未知错误"));
        } catch (Exception e) {
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
}
