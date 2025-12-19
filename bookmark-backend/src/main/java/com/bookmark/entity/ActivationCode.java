package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 激活码实体
 */
@Data
@TableName("activation_code")
public class ActivationCode {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code; // 激活码

    private Integer extraBookmarks; // 增加的书签数量

    private Integer extraCategories; // 增加的分类数量

    private Integer expireDays; // 有效天数

    private Integer maxUses; // 最大使用次数

    private Integer usedCount; // 已使用次数

    private Long createdBy; // 创建者ID

    private Integer status; // 0-禁用 1-启用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
