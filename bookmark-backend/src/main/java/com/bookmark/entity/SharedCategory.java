package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分享链接实体
 */
@Data
@TableName("shared_category")
public class SharedCategory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String shareCode; // 唯一分享码

    private String password; // 访问密码（可选）

    private LocalDateTime expireTime; // 过期时间

    private Integer viewCount; // 访问次数

    private Integer status; // 1-有效 0-已取消

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
