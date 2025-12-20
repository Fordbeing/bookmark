# 浏览器扩展下载和部署指南

## 本地开发环境

### 1. 打包扩展
运行打包脚本（已完成，文件已生成）：
```powershell
powershell -ExecutionPolicy Bypass -File "d:\bookmark\bookmark\package-extension.ps1"
```

这会创建：`d:\bookmark\bookmark\bookmark-frontend\public\bookmark-extension.zip`

### 2. 本地访问
开发环境下，用户可以通过以下地址下载：
```
http://localhost:5173/bookmark-extension.zip
```

### 3. 测试下载功能
1. 运行 `npm run dev`
2. 访问网页应用
3. 点击侧边栏「更多功能」→「浏览器插件」
4. 点击「下载扩展」按钮
5. 应该会自动下载 `bookmark-extension.zip`

---

## 生产环境部署

### 选项1：静态文件服务（推荐）

将扩展文件放到 Nginx 静态目录：

```bash
# 1. 打包扩展
powershell -ExecutionPolicy Bypass -File "package-extension.ps1"

# 2. 复制到生产服务器 (Windows)
scp d:\bookmark\bookmark\bookmark-frontend\public\bookmark-extension.zip root@115.159.219.125:/path/to/nginx/html/downloads/

# 3. 更新 ExtensionPage.vue 中的下载链接（生产环境）
# link.href = 'https://www.bookutil.top/downloads/bookmark-extension.zip';
```

### 选项2：通过后端 API 提供下载

创建一个下载接口：

**后端（Spring Boot）：**
```java
@GetMapping("/download/extension")
public ResponseEntity<Resource> downloadExtension() {
    Path filePath = Paths.get("extensions/bookmark-extension.zip");
    Resource resource = new FileSystemResource(filePath);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"bookmark-extension.zip\"")
        .body(resource);
}
```

**前端更新：**
```javascript
// ExtensionPage.vue
const downloadExtension = () => {
  window.location.href = '/api/download/extension';
};
```

---

## 自动化部署

### 添加到部署脚本

修改 `deploy.bat` 或 `deploy.ps1`，在部署前端时自动打包扩展：

```powershell
# 在前端构建前添加
Write-Host "正在打包浏览器扩展..." -ForegroundColor Green
powershell -ExecutionPolicy Bypass -File "package-extension.ps1"

# 然后继续前端构建
npm run build
```

---

## 更新扩展

当扩展有更新时：

1. 修改扩展代码
2. 更新 `manifest.json` 中的版本号
3. 重新运行打包脚本
4. 重新部署到服务器

用户需要在浏览器扩展管理页面点击「重新加载」按钮来更新扩展。

---

## 当前状态

✅ 打包脚本已创建：`package-extension.ps1`
✅ ZIP 文件已生成：`bookmark-frontend/public/bookmark-extension.zip`
✅ 前端下载功能已配置：`ExtensionPage.vue`
✅ 本地开发可用：`http://localhost:5173/bookmark-extension.zip`

**下一步**：在生产环境部署时，需要将 ZIP 文件部署到服务器并更新下载链接。
