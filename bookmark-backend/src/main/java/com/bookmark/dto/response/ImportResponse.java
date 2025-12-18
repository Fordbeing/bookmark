package com.bookmark.dto.response;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * 导入响应 DTO
 */
@Data
public class ImportResponse {
    /**
     * 成功导入的书签数量
     */
    private int successCount = 0;

    /**
     * 跳过的书签数量（重复或无效）
     */
    private int skippedCount = 0;

    /**
     * 失败的书签数量
     */
    private int failedCount = 0;

    /**
     * 创建的分类数量
     */
    private int categoriesCreated = 0;

    /**
     * 错误/警告信息列表
     */
    private List<String> messages = new ArrayList<>();

    public void addMessage(String message) {
        this.messages.add(message);
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public void incrementSkipped() {
        this.skippedCount++;
    }

    public void incrementFailed() {
        this.failedCount++;
    }

    public void incrementCategoriesCreated() {
        this.categoriesCreated++;
    }
}
