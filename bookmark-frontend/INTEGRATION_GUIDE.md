# Frontend-Backend Integration Guide

## 🎯 快速开始

### 1. 启动后端服务

```bash
cd bookmark-backend
mvn spring-boot:run
```

后端将运行在: `http://localhost:8080/api`

### 2. 启动前端服务

```bash
cd bookmark-frontend  
npm run dev
```

前端将运行在: `http://localhost:5173`

---

## ✅ 已完成的对接

### API模块
- ✅ `src/api/request.js` - Axios配置 + JWT拦截器
- ✅ `src/api/auth.js` - 登录/注册/用户信息
- ✅ `src/api/bookmark.js` - 书签CRUD操作
- ✅ `src/api/category.js` - 分类管理
- ✅ `src/api/settings.js` - 用户设置

### 组件更新
- ✅ `App.vue` - 使用真实书签API
- ✅ `AuthPage.vue` - 使用真实登录/注册API

---

## 🔌 API对接说明

### 认证流程

1. **注册/登录** → 后端返回JWT token
2. **保存token** → 存储到localStorage
3. **后续请求** → axios自动添加`Authorization: Bearer <token>`头

### 数据流

```
前端组件 → API调用 → axios拦截器(添加token) → 后端接口 → 返回结果 → axios拦截器(处理响应) → 前端组件
```

---

## 📝 使用示例

### 登录

```javascript
import { loginAPI } from './api/auth';

const login = async () => {
  const result = await loginAPI({
    email: 'test@example.com',
    password: '123456'
  });
  
  // 保存token
  localStorage.setItem('token', result.data.token);
};
```

### 创建书签

```javascript
import { createBookmarkAPI } from './api/bookmark';

const addBookmark = async () => {
  await createBookmarkAPI({
    title: 'Google',
    url: 'https://www.google.com',
    description: '搜索引擎'
  });
};
```

---

## ⚠️ 注意事项

1. **CORS配置**: 后端已配置允许`http://localhost:5173`跨域
2. **Token过期**: 401错误会自动清除token并提示重新登录
3. **错误处理**: 所有API错误都会通过ElMessage显示

---

## 🧪 测试步骤

1. 启动后端服务
2. 启动前端服务
3. 访问 http://localhost:5173
4. 点击"登录/注册"
5. 注册新用户或使用测试账号登录
6. 测试添加/编辑/删除书签功能

---

## 📊 后端API地址

- Base URL: `http://localhost:8080/api`
- Swagger文档: `http://localhost:8080/api/doc.html`

所有接口均已在前端对接完成，可以直接使用！🎉
