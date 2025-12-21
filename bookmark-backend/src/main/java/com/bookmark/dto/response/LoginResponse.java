package com.bookmark.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String phone;
    private String avatar;
    private Integer isAdmin;

    // 兼容旧版前端
    private String token;

    // 新增双 Token
    private String accessToken;
    private String refreshToken;

    // Token 有效期（秒）
    private Long expiresIn;
}
