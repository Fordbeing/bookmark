package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String password;

    private String phone; // 手机号(用于账户绑定)

    private String openid; // 微信小程序OpenID

    private String unionid; // 微信UnionID

    private Integer loginType; // 登录方式: 1-邮箱 2-微信 3-手机

    private String avatar;

    private String nickname;

    private Integer status; // 0-禁用 1-正常

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime lastLoginTime;
}
