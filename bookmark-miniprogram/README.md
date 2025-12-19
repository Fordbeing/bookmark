# 书签管理微信小程序

基于现有 Vue3 前端项目功能，开发的微信小程序版本。

## 功能特性

- 🔐 用户登录/注册（JWT 认证）
- 📚 书签管理（添加、编辑、删除、收藏）
- 📁 分类管理
- 🔍 搜索书签
- 🗑️ 回收站（恢复、永久删除）
- 👤 个人中心

## 项目结构

```
bookmark-miniprogram/
├── app.js              # 小程序入口
├── app.json            # 全局配置
├── app.wxss            # 全局样式
├── project.config.json # 项目配置
├── utils/
│   ├── request.js      # HTTP 请求封装
│   └── util.js         # 工具函数
├── api/
│   ├── auth.js         # 认证接口
│   ├── bookmark.js     # 书签接口
│   └── category.js     # 分类接口
├── pages/
│   ├── index/          # 首页-书签列表
│   ├── login/          # 登录注册
│   ├── add/            # 添加/编辑书签
│   ├── category/       # 分类管理
│   ├── trash/          # 回收站
│   └── profile/        # 个人中心
└── assets/
    └── icons/          # TabBar 图标
```

## 使用说明

### 1. 准备工作

确保后端服务已启动：

```bash
cd ../bookmark-backend
mvn spring-boot:run
```

后端运行在：`http://localhost:8080/api`

### 2. 配置 API 地址

编辑 `app.js`，修改 `baseUrl` 为你的后端地址：

```javascript
globalData: {
  baseUrl: 'http://localhost:8080/api'  // 修改为实际地址
}
```

> ⚠️ **注意**：真机调试时需要使用公网地址或内网穿透工具。

### 3. 导入项目

1. 下载并打开 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 点击「导入项目」
3. 选择本目录 `bookmark-miniprogram`
4. AppID 可使用测试号或自己的 AppID

### 4. 运行调试

- 编译后在模拟器中预览
- 点击「真机调试」在手机上测试

## 图标资源

TabBar 需要以下图标文件（81x81 像素 PNG）：

```
assets/icons/
├── bookmark.png          # 书签图标
├── bookmark-active.png   # 书签图标（选中）
├── category.png          # 分类图标
├── category-active.png   # 分类图标（选中）
├── trash.png             # 回收站图标
├── trash-active.png      # 回收站图标（选中）
├── profile.png           # 我的图标
└── profile-active.png    # 我的图标（选中）
```

> 可使用 [Iconfont](https://www.iconfont.cn/) 下载适合的图标。

## API 接口

与 Vue3 前端使用相同的后端接口：

| 功能 | 方法 | 路径 |
|------|------|------|
| 登录 | POST | /auth/login |
| 注册 | POST | /auth/register |
| 书签列表 | GET | /bookmarks |
| 创建书签 | POST | /bookmarks |
| 更新书签 | PUT | /bookmarks/:id |
| 删除书签 | DELETE | /bookmarks/:id |
| 分类列表 | GET | /categories |
| 创建分类 | POST | /categories |

## 注意事项

1. **链接处理**：微信小程序无法直接打开外部链接，点击书签会复制链接到剪贴板
2. **网络配置**：需要在小程序后台配置合法域名
3. **登录状态**：Token 存储在 `wx.storage` 中
