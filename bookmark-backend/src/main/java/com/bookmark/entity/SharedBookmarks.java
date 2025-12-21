package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 批量书签分享实体
 */
@Data
@TableName("shared_bookmarks")
public class SharedBookmarks {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title; // 分享标题

    private String bookmarkIds; // 逗号分隔的书签ID列表

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
