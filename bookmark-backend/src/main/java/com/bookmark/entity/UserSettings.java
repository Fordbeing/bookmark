package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_settings")
public class UserSettings {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String theme; // light/dark/auto

    private String primaryColor;

    private String secondaryColor;

    private String accentColor;

    private String backgroundColor;

    private String sidebarColorFrom;

    private String sidebarColorTo;

    private String displayMode; // card/list/compact

    private Integer autoOpenNewTab;

    private Integer showStats;

    private Integer defaultBookmarkLimit; // 书签上限

    private Integer defaultCategoryLimit; // 分类上限

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
