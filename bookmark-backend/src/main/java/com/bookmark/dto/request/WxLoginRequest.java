package com.bookmark.dto.request;

import lombok.Data;

/**
 * 微信登录请求DTO
 */
@Data
public class WxLoginRequest {
    /**
     * wx.login() 返回的临时登录凭证code
     */
    private String code;

    /**
     * 用户昵称(可选,用于创建新用户)
     */
    private String nickName;

    /**
     * 用户头像URL(可选)
     */
    private String avatarUrl;
}
