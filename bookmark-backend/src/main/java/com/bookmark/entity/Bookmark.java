package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("bookmark")
public class Bookmark {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String url;

    private String description;

    private String iconUrl;

    private Long categoryId;

    private String tags; // JSON数组

    private Integer isFavorite; // 0-否 1-是

    private Integer visitCount;

    private Integer sortOrder;

    @TableLogic
    private Integer status; // 0-删除 1-正常

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
