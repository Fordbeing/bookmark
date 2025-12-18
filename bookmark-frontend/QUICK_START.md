# 🚀 快速入门指南

## 项目运行

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 生产构建
npm run build
```

访问地址：http://localhost:5174

---

## 🎨 页面布局说明

### 整体结构
```
┌─────────────────────────────────────────────┐
│  Navbar (顶部导航栏)                         │
├────────────┬──────────────────────────────────┤
│            │                                  │
│  Sidebar   │  主内容区域                      │
│ (侧边栏)   │  - 欢迎区域                      │
│            │  - 快速添加                      │
│ - 分类列表 │  - 书签网格                      │
│ - 快速操作 │                                  │
│ - 设置入口 │                                  │
│            │                                  │
└────────────┴──────────────────────────────────┘
```

---

## 📦 主要组件说明

### 1. **Sidebar 侧边栏**
**位置**：`src/components/Sidebar.vue`

**功能**：
- 显示书签分类（工作、学习、娱乐、生活）
- 显示统计信息（总书签数、分类数）
- 快速操作入口（所有书签、星标、回收站）
- 设置和个人中心链接

**使用**：
```vue
<Sidebar />
```

---

### 2. **Navbar 顶部导航栏**
**位置**：`src/components/Navbar.vue`

**功能**：
- 系统标题和副标题
- 搜索框（用于查询书签）
- 快速操作按钮

**使用**：
```vue
<Navbar />
```

---

### 3. **AddBookmarkModal 添加书签**
**位置**：`src/components/AddBookmarkModal.vue`

**功能**：
- 弹窗形式的书签添加表单
- 支持标题、链接、分类、描述、标签填写
- 表单验证

**使用**：
```vue
<AddBookmarkModal 
  :visible="isModalVisible"
  @update:visible="isModalVisible = $event"
  @close="toggleModal" 
  @add-bookmark="handleBookmarkAdded" 
/>
```

**主要props**：
- `visible` (Boolean)：控制弹窗显示/隐藏

**主要事件**：
- `add-bookmark`：用户提交表单时触发
- `close`：用户关闭弹窗时触发

---

### 4. **SettingsPage 设置页面**
**位置**：`src/components/SettingsPage.vue`

**功能**：
- 通用设置（主题、显示方式等）
- 分类管理（添加/删除分类）
- 快捷键查看
- 数据管理（导入/导出）
- 关于信息

**使用**：
```vue
<SettingsPage 
  :visible="isSettingsVisible"
  @update:visible="isSettingsVisible = $event"
  @close="isSettingsVisible = false"
/>
```

---

## 📊 书签数据结构

```javascript
const bookmark = {
  id: 1,                              // 唯一ID
  title: 'Google',                    // 书签标题
  url: 'https://www.google.com',      // 网址
  description: '搜索引擎',             // 描述
  iconUrl: 'https://...',             // 图标URL
  createTime: '2025-12-17T...',       // 创建时间（ISO字符串）
  category: '工作',                   // 分类
  tags: ['搜索', '工具']               // 标签（可选）
};
```

---

## 🎯 常见操作

### 添加书签

**方式1：快速添加**
1. 在主内容区顶部输入框输入链接
2. 点击"立即添加"或按Enter

**方式2：详细添加**
1. 点击右下角"+"按钮或"详细添加"
2. 在弹窗中填写完整信息
3. 点击"保存"

### 打开书签

直接点击书签卡片即可在新标签页打开网址

### 编辑书签

点击书签卡片右下角的"✏️"按钮（功能开发中）

### 删除书签

点击书签卡片右下角的"🗑️"按钮即可删除

### 打开设置

点击侧边栏底部的"⚙️ 设置"按钮

---

## 🔄 状态管理

所有主要状态都在 `App.vue` 中使用 `ref` 管理：

```javascript
const isModalVisible = ref(false);      // 添加书签弹窗显示状态
const isSettingsVisible = ref(false);   // 设置页面显示状态
const bookmarks = ref([...]);           // 书签列表
const inputUrl = ref('');               // 输入框URL
const loading = ref(false);             // 加载状态
```

---

## 🎨 样式定制

项目使用 **Tailwind CSS** 进行样式定制。

主要颜色变量：
- 主色（蓝色）：`bg-blue-600`, `bg-blue-800`
- 文本色：`text-gray-800`, `text-gray-600`, `text-gray-500`
- 背景色：`bg-gray-50`
- 白色背景：`bg-white`

如需修改颜色方案，可以在组件中更新Tailwind类名。

---

## 📱 响应式设计

页面已针对以下断点进行优化：

- **小屏幕**（< 768px）：1列布局 + 隐藏侧边栏（可选）
- **中等屏幕**（768px - 1024px）：2列书签 + 缩小侧边栏
- **大屏幕**（> 1024px）：3列书签 + 完整侧边栏

---

## 🔗 API集成

项目中已有API调用框架（在 `src/api/bookmark.js` 中）：

```javascript
// 获取书签列表
await getBookmarkListAPI();

// 添加书签
await addBookmarkAPI(url);
```

目前这些API返回本地数据，可根据需要替换为真实API调用。

---

## 🐛 常见问题

### Q: 添加书签后没有显示？
A: 检查 `handleBookmarkAdded` 方法是否正确更新了 `bookmarks` 列表。

### Q: 搜索框不工作？
A: 搜索功能还在开发中，暂未实现搜索逻辑。

### Q: 设置无法保存？
A: 设置功能框架已完成，需实现 localStorage 或后端存储逻辑。

### Q: 分类点击无反应？
A: 分类过滤功能还在开发中。

---

## 📚 后续开发提示

### 立即可做的功能
1. ✅ 连接真实API（替换 `src/api/bookmark.js`）
2. ✅ 实现 localStorage 本地存储
3. ✅ 完成分类过滤逻辑
4. ✅ 实现搜索功能

### 中期开发
1. ✅ 实现书签编辑功能
2. ✅ 添加星标/收藏功能
3. ✅ 实现导入/导出功能
4. ✅ 添加暗黑主题

### 长期规划
1. ✅ 用户认证系统
2. ✅ 云同步功能
3. ✅ 浏览器扩展
4. ✅ 高级统计功能

---

## 📞 技术支持

如有问题，请检查：
1. 控制台错误信息（按F12打开开发者工具）
2. 网络请求状态（Network标签）
3. 组件状态（Vue插件）
4. 相关源码注释

---

**最后更新**：2025年12月17日  
**版本**：1.0.0-alpha
