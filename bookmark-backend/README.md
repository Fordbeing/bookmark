# Bookmark Management System - Backend

书签管理系统后端服务 - 基于 Spring Boot 3.x

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring Security + JWT
- MySQL 8.0+
- MyBatis Plus 3.5.5
- Redis (可选)

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库初始化

```bash
# 连接MySQL
mysql -u root -p

# 执行SQL脚本
source schema.sql
```

或者直接在MySQL中运行：
```sql
CREATE DATABASE bookmark_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookmark_db;
-- 然后执行 schema.sql 中的表创建语句
```

### 3. 配置文件

修改 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookmark_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的MySQL密码
```

### 4. 启动项目

```bash
# 使用Maven启动
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/bookmark-backend-1.0.0.jar
```

### 5. 访问API文档

启动成功后，访问：
- Swagger UI: http://localhost:8080/api/doc.html
- 应用端口: http://localhost:8080/api

## 项目结构

```
bookmark-backend/
├── src/main/java/com/bookmark/
│   ├── BookmarkApplication.java          # 启动类
│   ├── config/                           # 配置类
│   │   ├── CorsConfig.java              # 跨域配置
│   │   ├── MyMetaObjectHandler.java     # MyBatis自动填充
│   │   └── SecurityConfig.java          # 安全配置
│   ├── controller/                       # 控制器
│   │   ├── AuthController.java          # 认证接口
│   │   ├── BookmarkController.java      # 书签接口
│   │   ├── CategoryController.java      # 分类接口
│   │   └── SettingsController.java      # 设置接口
│   ├── dto/                             # 数据传输对象
│   │   ├── request/                     # 请求DTO
│   │   └── response/                    # 响应DTO
│   ├── entity/                          # 实体类
│   ├── exception/                       # 异常处理
│   │   └── GlobalExceptionHandler.java
│   ├── mapper/                          # MyBatis Mapper
│   ├── security/                        # 安全相关
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   └── UserDetailsServiceImpl.java
│   ├── service/                         # 服务接口
│   └── service/impl/                    # 服务实现
└── src/main/resources/
    └── application.yml                   # 配置文件
```

## API接口

### 认证模块 `/auth`

- `POST /auth/register` - 用户注册
- `POST /auth/login` - 用户登录
- `GET /auth/me` - 获取当前用户信息
- `POST /auth/logout` - 退出登录

### 书签模块 `/bookmarks`

- `GET /bookmarks` - 获取书签列表（分页）
- `GET /bookmarks/{id}` - 获取书签详情
- `POST /bookmarks` - 创建书签
- `PUT /bookmarks/{id}` - 更新书签
- `DELETE /bookmarks/{id}` - 删除书签
- `DELETE /bookmarks/batch` - 批量删除
- `PUT /bookmarks/{id}/favorite` - 收藏/取消收藏
- `PUT /bookmarks/{id}/visit` - 记录访问

### 分类模块 `/categories`

- `GET /categories` - 获取分类列表
- `POST /categories` - 创建分类
- `PUT /categories/{id}` - 更新分类
- `DELETE /categories/{id}` - 删除分类

### 设置模块 `/settings`

- `GET /settings` - 获取用户设置
- `PUT /settings` - 更新用户设置

## 测试用户

默认测试用户（如果执行了schema.sql中的测试数据）:
- 邮箱: `admin@example.com`
- 密码: `123456`

## 开发说明

### 添加新接口

1. 在对应的 Service 接口中定义方法
2. 在 ServiceImpl 中实现业务逻辑
3. 在 Controller 中添加API端点
4. 添加必要的 DTO 类

### 数据验证

使用 Jakarta Validation 注解进行参数验证：
- `@NotBlank` - 不能为空
- `@Email` - 邮箱格式
- `@Size` - 长度限制
- `@Pattern` - 正则表达式

### 权限控制

所有需要认证的接口都需要在请求头中携带JWT Token：
```
Authorization: Bearer <your_jwt_token>
```

## 常见问题

### 1. 数据库连接失败

检查MySQL是否启动，用户名密码是否正确。

### 2. 端口被占用

修改 `application.yml` 中的 `server.port`。

### 3. JWT验证失败

检查token是否过期，或者secret key是否正确。

## 许可证

MIT License
