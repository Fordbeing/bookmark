package com.bookmark.dto.request;

import lombok.Data;

/**
 * 绑定手机号请求DTO
 */
@Data
public class BindPhoneRequest {
    /**
     * 手机号
     */
    private String phone;

    /**
     * 验证码(如果启用短信验证)
     */
    private String code;
}
