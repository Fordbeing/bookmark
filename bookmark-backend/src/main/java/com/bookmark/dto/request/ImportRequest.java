package com.bookmark.dto.request;

import lombok.Data;

/**
 * 导入请求 DTO
 */
@Data
public class ImportRequest {
    /**
     * 导入类型: CHROME, EDGE, JSON
     */
    private String importType;

    /**
     * 文件内容（Base64 编码或原始内容）
     */
    private String content;
}
