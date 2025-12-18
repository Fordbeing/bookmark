package com.bookmark.controller;

import com.bookmark.service.UrlMetadataService;
import com.bookmark.service.UrlMetadataService.UrlMetadata;
import com.bookmark.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * URL 元数据控制器
 */
@RestController
@RequestMapping("/url")
@RequiredArgsConstructor
public class UrlMetadataController {

    private final UrlMetadataService urlMetadataService;

    /**
     * 获取 URL 的元数据（标题、描述、图标）
     */
    @GetMapping("/metadata")
    public Result<UrlMetadata> fetchMetadata(@RequestParam String url) {
        if (url == null || url.trim().isEmpty()) {
            return Result.error(400, "URL 不能为空");
        }

        UrlMetadata metadata = urlMetadataService.fetchMetadata(url.trim());
        return Result.success(metadata);
    }
}
