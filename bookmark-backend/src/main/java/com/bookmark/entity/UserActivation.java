package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户激活记录实体
 */
@Data
@TableName("user_activation")
public class UserActivation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId; // 用户ID

    private Long codeId; // 激活码ID

    private Integer extraBookmarks; // 增加的书签数量

    private Integer extraCategories; // 增加的分类数量

    private LocalDateTime expireTime; // 过期时间

    private Integer status; // 0-已过期 1-生效中

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
