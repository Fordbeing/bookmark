package com.bookmark.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体
 */
@Data
@TableName("announcement")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String type; // info/warning/maintenance/update (ENUM)
    private Integer status; // 0-草稿 1-已发布

    @TableField("created_by")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("end_time")
    private LocalDateTime expireTime;

    // publishTime 不在数据库中，标记为不存在
    @TableField(exist = false)
    private LocalDateTime publishTime;
}
