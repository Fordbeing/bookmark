# 前后端完整对接说明

## ✅ 已完成的对接

### 1. API模块创建
- ✅ `src/api/request.js` - Axios配置 + JWT token拦截器
- ✅ `src/api/auth.js` - 登录/注册API  
- ✅ `src/api/bookmark.js` - 书签CRUD API
- ✅ `src/api/category.js` - 分类管理API
- ✅ `src/api/settings.js` - 用户设置API

### 2. 登录注册功能
- ✅ AuthPage使用真实API
- ✅ 登录后保存JWT token到localStorage
- ✅ token自动添加到后续请求头

### 3. 书签管理功能
- ✅ 加载书签列表（App.vue的fetchList）
- ✅ 创建书签（调用createBookmarkAPI）
- ✅ 更新书签（调用updateBookmarkAPI）
- ✅ 删除书签（调用deleteBookmarkAPI）

### 4. 分类筛选功能  
- ✅ Sidebar加载真实分类列表
- ✅ 显示每个分类的书签数量
- ✅ 点击分类筛选对应书签
- ✅ 点击"所有书签"显示全部
- ✅ 点击"星标书签"筛选收藏

### 5. 统计数据显示
- ✅ 侧边栏显示总书签数（动态）
- ✅ 侧边栏显示分类数量（动态）
- ✅ 书签列表标题显示当前筛选结果数量

## 🔧 使用前准备

### 1. 数据库初始化

**必须先创建数据库！**

```sql
-- 创建数据库
CREATE DATABASE bookmark_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE bookmark_db;

-- 然后执行 bookmark-backend/schema.sql 中的所有建表语句
```

### 2. 启动后端

```bash
cd bookmark-backend
mvn spring-boot:run
```

后端运行在：`http://localhost:8080/api`

### 3. 启动前端

```bash
cd bookmark-frontend  
npm run dev
```

前端运行在：`http://localhost:5173`

## 📝 完整使用流程

1. **注册账号**
   - 点击右上角"登录/注册"
   - 填写用户名、邮箱、密码
   - 点击注册

2. **登录系统**
   - 输入邮箱和密码
   - 系统自动保存token
   - 登录成功后自动加载书签

3. **添加书签**
   - 快速添加：粘贴URL点击"立即添加"
   - 手工添加：点击"手工添加"填写详细信息

4. **使用分类筛选**
   - 左侧边栏点击分类名称
   - 主区域只显示该分类的书签
   - 书签数量会实时更新

5. **星标筛选**
   - 点击左侧"星标书签"
   - 只显示已收藏的书签

6. **查看统计**
   - 左侧栏顶部查看总书签数
   - 查看总分类数

## 🎯 数据流向

```
用户操作 → 前端组件方法 → API调用 → axios拦截器(添加token) 
→ Spring Boot Controller → Service → Mapper → MySQL数据库
→ 返回数据 → axios拦截器(处理响应) → 更新前端状态 → UI更新
```

## 🐛 常见问题

1. **登录后看不到书签**
   - 检查后端是否正常运行
   - 查看浏览器console是否有API错误
   - 确认token是否已保存（localStorage查看）

2. **分类不显示**
   - 需要先创建分类
   - 目前需要通过API手动创建或修改数据库

3. **书签数量显示0**
   - 确认数据库有数据
   - 检查categoryId是否匹配

## 💡 技术要点

- **认证**：JWT token存储在localStorage，每次请求自动添加
- **状态管理**：使用Vue3 ref和computed实现响应式
- **过滤逻辑**：currentFilter控制过滤类型，filteredBookmarks动态计算结果
- **数据同步**：增删改后调用fetchList重新加载数据

全部功能已对接完成！🎉
