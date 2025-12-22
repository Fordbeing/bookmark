package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户登录历史实体
 */
@Data
@TableName("login_history")
public class LoginHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId; // 用户ID

    private String ipAddress; // IP地址

    private String location; // 地理位置

    private String device; // 设备信息

    private String browser; // 浏览器信息

    private Integer status; // 1-成功 0-失败

    private String failReason; // 失败原因

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
