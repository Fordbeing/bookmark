# 打包浏览器扩展为 ZIP 文件
# 用于分发给用户下载

$sourceDir = "d:\bookmark\bookmark\bookmark-extension"
$outputFile = "d:\bookmark\bookmark\bookmark-frontend\public\bookmark-extension.zip"

# 确保 public 目录存在
$publicDir = "d:\bookmark\bookmark\bookmark-frontend\public"
if (-not (Test-Path $publicDir)) {
    New-Item -ItemType Directory -Path $publicDir -Force
}

# 删除旧的 zip 文件（如果存在）
if (Test-Path $outputFile) {
    Remove-Item $outputFile -Force
}

Write-Host "正在打包扩展文件..." -ForegroundColor Green

# 压缩扩展目录
Compress-Archive -Path "$sourceDir\*" -DestinationPath $outputFile -Force

$fileSize = (Get-Item $outputFile).Length / 1KB
Write-Host "✓ 扩展已打包: $outputFile" -ForegroundColor Green
Write-Host "  文件大小: $([math]::Round($fileSize, 2)) KB" -ForegroundColor Cyan

Write-Host "`n提示: 压缩包已保存到 public 目录，可通过前端访问" -ForegroundColor Yellow
Write-Host "访问地址: http://localhost:5173/bookmark-extension.zip" -ForegroundColor Yellow
