package com.bookmark.controller;

import com.bookmark.dto.request.ImportRequest;
import com.bookmark.dto.response.ExportResponse;
import com.bookmark.dto.response.ImportResponse;
import com.bookmark.service.DataManagementService;
import com.bookmark.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 数据管理控制器
 */
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class DataManagementController {

    private final DataManagementService dataManagementService;

    /**
     * 导出所有用户数据
     */
    @GetMapping("/export")
    public Result<ExportResponse> exportData() {
        ExportResponse response = dataManagementService.exportData();
        return Result.success("导出成功", response);
    }

    /**
     * 导入数据（通过 JSON body）
     */
    @PostMapping("/import")
    public Result<ImportResponse> importData(@RequestBody ImportRequest request) {
        ImportResponse response = dataManagementService.importData(request);

        if (response.getSuccessCount() > 0) {
            String message = String.format("导入成功！新增 %d 个书签，创建 %d 个分类",
                    response.getSuccessCount(), response.getCategoriesCreated());
            if (response.getSkippedCount() > 0) {
                message += String.format("，跳过 %d 个重复项", response.getSkippedCount());
            }
            return Result.success(message, response);
        } else if (!response.getMessages().isEmpty()) {
            return Result.error(400, response.getMessages().get(0));
        } else {
            return Result.success("没有需要导入的新书签", response);
        }
    }

    /**
     * 通过文件上传导入数据
     */
    @PostMapping("/import/file")
    public Result<ImportResponse> importFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String importType) {

        if (file.isEmpty()) {
            return Result.error(400, "请选择要导入的文件");
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            ImportRequest request = new ImportRequest();
            request.setImportType(importType);
            request.setContent(content);

            ImportResponse response = dataManagementService.importData(request);

            if (response.getSuccessCount() > 0) {
                String message = String.format("导入成功！新增 %d 个书签，创建 %d 个分类",
                        response.getSuccessCount(), response.getCategoriesCreated());
                if (response.getSkippedCount() > 0) {
                    message += String.format("，跳过 %d 个重复项", response.getSkippedCount());
                }
                return Result.success(message, response);
            } else if (!response.getMessages().isEmpty()) {
                return Result.error(400, response.getMessages().get(0));
            } else {
                return Result.success("没有需要导入的新书签", response);
            }
        } catch (IOException e) {
            return Result.error(500, "文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有用户数据（需要密码验证）
     */
    @DeleteMapping("/clear")
    public Result<Void> clearAllData(@RequestBody java.util.Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            return Result.error(400, "请输入密码");
        }

        boolean success = dataManagementService.clearAllDataWithPasswordVerification(password);
        if (success) {
            return Result.success("所有数据已清除", null);
        } else {
            return Result.error(401, "密码错误");
        }
    }
}
