package com.bookmark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BookmarkRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255")
    private String title;

    @NotBlank(message = "URL不能为空")
    @Pattern(regexp = "^https?://.*", message = "URL格式不正确")
    @Size(max = 2048, message = "URL长度不能超过2048")
    private String url;

    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    private Long categoryId;

    private List<String> tags;

    private Integer isFavorite;
}
