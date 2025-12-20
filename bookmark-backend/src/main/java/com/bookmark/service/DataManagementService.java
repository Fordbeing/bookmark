package com.bookmark.service;

import com.bookmark.dto.request.ImportRequest;
import com.bookmark.dto.response.ExportResponse;
import com.bookmark.dto.response.ImportResponse;

/**
 * 数据管理服务接口
 */
public interface DataManagementService {

    /**
     * 导出用户所有数据
     * 
     * @return 导出响应（包含书签、分类、标签）
     */
    ExportResponse exportData();

    /**
     * 导入数据
     * 
     * @param request 导入请求
     * @return 导入响应（包含统计信息和错误列表）
     */
    ImportResponse importData(ImportRequest request);

    /**
     * 清除用户所有数据
     */
    void clearAllData();

    /**
     * 清除用户所有数据（带密码验证）
     * 
     * @param password 用户密码
     * @return 验证是否成功
     */
    boolean clearAllDataWithPasswordVerification(String password);
}
