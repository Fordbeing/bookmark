# TabBar 图标资源

请在此目录放置以下图标文件（81x81 像素 PNG 格式）：

## 必需图标

| 文件名 | 说明 |
|--------|------|
| bookmark.png | 书签（未选中） |
| bookmark-active.png | 书签（选中） |
| category.png | 分类（未选中） |
| category-active.png | 分类（选中） |
| trash.png | 回收站（未选中） |
| trash-active.png | 回收站（选中） |
| profile.png | 我的（未选中） |
| profile-active.png | 我的（选中） |

## 获取图标

推荐使用以下网站下载免费图标：

1. [Iconfont](https://www.iconfont.cn/) - 阿里巴巴图标库
2. [Flaticon](https://www.flaticon.com/) - 免费图标
3. [Icons8](https://icons8.com/) - 设计图标

## 颜色建议

- 未选中颜色：#999999
- 选中颜色：#2563eb（主题蓝色）

## 临时解决方案

如果暂时没有图标，可以在 `app.json` 中移除 `iconPath` 和 `selectedIconPath` 配置，TabBar 将只显示文字。
