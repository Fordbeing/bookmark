# 书签管理系统 - 后端架构设计文档

## 📋 目录
- [技术栈](#技术栈)
- [数据库设计](#数据库设计)
- [API接口设计](#api接口设计)
- [项目结构](#项目结构)
- [安全设计](#安全设计)
- [部署建议](#部署建议)

---

## 🛠 技术栈

### 核心框架
- **Java 17+** - 编程语言
- **Spring Boot 3.x** - 后端框架
- **Spring Security** - 安全认证
- **JWT (JSON Web Token)** - 用户认证令牌
- **MySQL 8.0+** - 关系型数据库
- **MyBatis Plus** - ORM框架（简化CRUD操作）
- **Redis** - 缓存和Session管理

### 工具库
- **Lombok** - 简化Java代码
- **Hutool** - Java工具类库
- **Jackson** - JSON序列化
- **Validation** - 参数校验
- **Swagger/Knife4j** - API文档

### 构建工具
- **Maven** - 项目管理

---

## 🗄️ 数据库设计

### 1. 用户表 (user)
```sql
CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `avatar` VARCHAR(500) COMMENT '头像URL',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` DATETIME COMMENT '最后登录时间',
  INDEX idx_email (`email`),
  INDEX idx_username (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 2. 书签表 (bookmark)
```sql
CREATE TABLE `bookmark` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '书签ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NOT NULL COMMENT '书签标题',
  `url` VARCHAR(2048) NOT NULL COMMENT '书签URL',
  `description` TEXT COMMENT '描述',
  `icon_url` VARCHAR(500) COMMENT '网站图标URL',
  `category_id` BIGINT COMMENT '分类ID',
  `tags` VARCHAR(500) COMMENT '标签(JSON数组)',
  `is_favorite` TINYINT DEFAULT 0 COMMENT '是否收藏: 0-否 1-是',
  `visit_count` INT DEFAULT 0 COMMENT '访问次数',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-删除 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_category_id (`category_id`),
  INDEX idx_create_time (`create_time`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书签表';
```

### 3. 分类表 (category)
```sql
CREATE TABLE `category` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `color` VARCHAR(20) COMMENT '分类颜色',
  `icon` VARCHAR(50) COMMENT '分类图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  UNIQUE KEY uk_user_name (`user_id`, `name`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';
```

### 4. 用户设置表 (user_settings)
```sql
CREATE TABLE `user_settings` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
  `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
  `theme` VARCHAR(20) DEFAULT 'light' COMMENT '主题: light/dark/auto',
  `primary_color` VARCHAR(20) DEFAULT '#2563eb' COMMENT '主色',
  `secondary_color` VARCHAR(20) DEFAULT '#1e40af' COMMENT '辅助色',
  `accent_color` VARCHAR(20) DEFAULT '#f59e0b' COMMENT '强调色',
  `background_color` VARCHAR(20) DEFAULT '#ffffff' COMMENT '背景色',
  `sidebar_color_from` VARCHAR(20) DEFAULT '#2563eb' COMMENT '侧边栏渐变起始色',
  `sidebar_color_to` VARCHAR(20) DEFAULT '#1e3a8a' COMMENT '侧边栏渐变结束色',
  `display_mode` VARCHAR(20) DEFAULT 'card' COMMENT '显示模式: card/list/compact',
  `auto_open_new_tab` TINYINT DEFAULT 1 COMMENT '自动新标签打开',
  `show_stats` TINYINT DEFAULT 1 COMMENT '显示统计信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';
```

### 5. 标签表 (tag) - 可选
```sql
CREATE TABLE `tag` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `color` VARCHAR(20) COMMENT '标签颜色',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (`user_id`),
  UNIQUE KEY uk_user_name (`user_id`, `name`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';
```

### 6. 书签标签关联表 (bookmark_tag) - 可选
```sql
CREATE TABLE `bookmark_tag` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
  `bookmark_id` BIGINT NOT NULL COMMENT '书签ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_bookmark_tag (`bookmark_id`, `tag_id`),
  FOREIGN KEY (`bookmark_id`) REFERENCES `bookmark`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书签标签关联表';
```

---

## 🔌 API接口设计

### 基础配置
- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token (放在请求头 `Authorization: Bearer <token>`)
- **响应格式**: 统一JSON格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

---

### 1. 用户认证模块 (`/api/auth`)

#### 1.1 用户注册
```
POST /api/auth/register
Content-Type: application/json

Request:
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}

Response:
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### 1.2 用户登录
```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "email": "test@example.com",
  "password": "password123",
  "remember": true
}

Response:
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "avatar": null,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### 1.3 退出登录
```
POST /api/auth/logout
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "退出成功"
}
```

#### 1.4 获取当前用户信息
```
GET /api/auth/me
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "avatar": null,
    "nickname": "测试用户"
  }
}
```

---

### 2. 书签模块 (`/api/bookmarks`)

#### 2.1 获取书签列表
```
GET /api/bookmarks?page=1&size=20&categoryId=1&keyword=搜索关键词
Authorization: Bearer <token>

Query Parameters:
- page: 页码(默认1)
- size: 每页数量(默认20)
- categoryId: 分类ID(可选)
- keyword: 搜索关键词(可选)
- sortBy: 排序字段(create_time/visit_count)
- sortOrder: 排序方式(asc/desc)

Response:
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "title": "Google",
        "url": "https://www.google.com",
        "description": "搜索引擎",
        "iconUrl": "https://www.google.com/favicon.ico",
        "category": "工作",
        "categoryId": 1,
        "isFavorite": false,
        "visitCount": 10,
        "createTime": "2024-01-01 10:00:00",
        "updateTime": "2024-01-01 10:00:00"
      }
    ]
  }
}
```

#### 2.2 获取单个书签详情
```
GET /api/bookmarks/{id}
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "Google",
    "url": "https://www.google.com",
    "description": "搜索引擎",
    "iconUrl": "https://www.google.com/favicon.ico",
    "categoryId": 1,
    "category": "工作",
    "tags": ["搜索", "工具"],
    "isFavorite": false,
    "visitCount": 10,
    "createTime": "2024-01-01 10:00:00"
  }
}
```

#### 2.3 创建书签
```
POST /api/bookmarks
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "title": "Google",
  "url": "https://www.google.com",
  "description": "搜索引擎",
  "categoryId": 1,
  "tags": ["搜索", "工具"]
}

Response:
{
  "code": 200,
  "message": "书签创建成功",
  "data": {
    "id": 1,
    "title": "Google",
    "url": "https://www.google.com",
    "iconUrl": "https://www.google.com/favicon.ico",
    "createTime": "2024-01-01 10:00:00"
  }
}
```

#### 2.4 更新书签
```
PUT /api/bookmarks/{id}
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "title": "Google Search",
  "description": "全球最大的搜索引擎",
  "categoryId": 2
}

Response:
{
  "code": 200,
  "message": "书签更新成功"
}
```

#### 2.5 删除书签
```
DELETE /api/bookmarks/{id}
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "书签删除成功"
}
```

#### 2.6 批量删除书签
```
DELETE /api/bookmarks/batch
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "ids": [1, 2, 3]
}

Response:
{
  "code": 200,
  "message": "删除成功"
}
```

#### 2.7 收藏/取消收藏书签
```
PUT /api/bookmarks/{id}/favorite
Authorization: Bearer <token>

Request:
{
  "isFavorite": true
}

Response:
{
  "code": 200,
  "message": "操作成功"
}
```

#### 2.8 记录访问次数
```
PUT /api/bookmarks/{id}/visit
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "visitCount": 11
  }
}
```

---

### 3. 分类模块 (`/api/categories`)

#### 3.1 获取分类列表
```
GET /api/categories
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "工作",
      "color": "#2563eb",
      "icon": "💼",
      "bookmarkCount": 15,
      "createTime": "2024-01-01 10:00:00"
    }
  ]
}
```

#### 3.2 创建分类
```
POST /api/categories
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "name": "学习",
  "color": "#16a34a",
  "icon": "📚"
}

Response:
{
  "code": 200,
  "message": "分类创建成功",
  "data": {
    "id": 2,
    "name": "学习"
  }
}
```

#### 3.3 更新分类
```
PUT /api/categories/{id}
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "name": "学习资料",
  "color": "#16a34a"
}

Response:
{
  "code": 200,
  "message": "分类更新成功"
}
```

#### 3.4 删除分类
```
DELETE /api/categories/{id}
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "分类删除成功"
}
```

---

### 4. 设置模块 (`/api/settings`)

#### 4.1 获取用户设置
```
GET /api/settings
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "theme": "light",
    "primaryColor": "#2563eb",
    "secondaryColor": "#1e40af",
    "accentColor": "#f59e0b",
    "backgroundColor": "#ffffff",
    "sidebarColorFrom": "#2563eb",
    "sidebarColorTo": "#1e3a8a",
    "displayMode": "card",
    "autoOpenNewTab": true,
    "showStats": true
  }
}
```

#### 4.2 更新用户设置
```
PUT /api/settings
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "theme": "dark",
  "primaryColor": "#9333ea",
  "displayMode": "list"
}

Response:
{
  "code": 200,
  "message": "设置更新成功"
}
```

---

### 5. 数据管理模块 (`/api/data`)

#### 5.1 导出书签数据
```
GET /api/data/export
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "bookmarks": [...],
    "categories": [...],
    "settings": {...},
    "exportTime": "2024-01-01 10:00:00"
  }
}
```

#### 5.2 导入书签数据
```
POST /api/data/import
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "bookmarks": [...],
  "categories": [...],
  "mergeMode": "replace" // replace/merge
}

Response:
{
  "code": 200,
  "message": "导入成功",
  "data": {
    "importedBookmarks": 50,
    "importedCategories": 5
  }
}
```

---

### 6. 统计模块 (`/api/stats`)

#### 6.1 获取统计数据
```
GET /api/stats
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "totalBookmarks": 100,
    "totalCategories": 5,
    "totalVisits": 1500,
    "favoriteCount": 20,
    "recentAdded": 10,
    "categoryStats": [
      {
        "categoryName": "工作",
        "count": 30
      }
    ]
  }
}
```

---

## 📁 项目结构

```
bookmark-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── bookmark/
│   │   │           ├── BookmarkApplication.java         # 启动类
│   │   │           ├── config/                          # 配置类
│   │   │           │   ├── SecurityConfig.java          # 安全配置
│   │   │           │   ├── JwtConfig.java               # JWT配置
│   │   │           │   ├── CorsConfig.java              # 跨域配置
│   │   │           │   ├── RedisConfig.java             # Redis配置
│   │   │           │   └── SwaggerConfig.java           # API文档配置
│   │   │           ├── controller/                      # 控制器层
│   │   │           │   ├── AuthController.java          # 认证接口
│   │   │           │   ├── BookmarkController.java      # 书签接口
│   │   │           │   ├── CategoryController.java      # 分类接口
│   │   │           │   ├── SettingsController.java      # 设置接口
│   │   │           │   ├── DataController.java          # 数据管理接口
│   │   │           │   └── StatsController.java         # 统计接口
│   │   │           ├── entity/                          # 实体类
│   │   │           │   ├── User.java
│   │   │           │   ├── Bookmark.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── UserSettings.java
│   │   │           │   ├── Tag.java
│   │   │           │   └── BookmarkTag.java
│   │   │           ├── dto/                             # 数据传输对象
│   │   │           │   ├── request/
│   │   │           │   │   ├── LoginRequest.java
│   │   │           │   │   ├── RegisterRequest.java
│   │   │           │   │   ├── BookmarkRequest.java
│   │   │           │   │   └── CategoryRequest.java
│   │   │           │   └── response/
│   │   │           │       ├── LoginResponse.java
│   │   │           │       ├── BookmarkResponse.java
│   │   │           │       └── PageResponse.java
│   │   │           ├── mapper/                          # MyBatis Mapper
│   │   │           │   ├── UserMapper.java
│   │   │           │   ├── BookmarkMapper.java
│   │   │           │   ├── CategoryMapper.java
│   │   │           │   └── UserSettingsMapper.java
│   │   │           ├── service/                         # 服务层接口
│   │   │           │   ├── UserService.java
│   │   │           │   ├── BookmarkService.java
│   │   │           │   ├── CategoryService.java
│   │   │           │   ├── SettingsService.java
│   │   │           │   └── DataService.java
│   │   │           ├── service/impl/                    # 服务层实现
│   │   │           │   ├── UserServiceImpl.java
│   │   │           │   ├── BookmarkServiceImpl.java
│   │   │           │   ├── CategoryServiceImpl.java
│   │   │           │   ├── SettingsServiceImpl.java
│   │   │           │   └── DataServiceImpl.java
│   │   │           ├── security/                        # 安全相关
│   │   │           │   ├── JwtTokenProvider.java        # JWT工具类
│   │   │           │   ├── JwtAuthenticationFilter.java # JWT过滤器
│   │   │           │   └── UserDetailsServiceImpl.java  # 用户详情服务
│   │   │           ├── exception/                       # 异常处理
│   │   │           │   ├── GlobalExceptionHandler.java  # 全局异常处理
│   │   │           │   ├── BusinessException.java       # 业务异常
│   │   │           │   └── ErrorCode.java               # 错误码枚举
│   │   │           └── util/                            # 工具类
│   │   │               ├── Result.java                  # 统一响应
│   │   │               ├── PasswordUtil.java            # 密码工具
│   │   │               └── IconFetchUtil.java           # 图标获取工具
│   │   └── resources/
│   │       ├── application.yml                          # 主配置文件
│   │       ├── application-dev.yml                      # 开发环境配置
│   │       ├── application-prod.yml                     # 生产环境配置
│   │       └── mapper/                                  # MyBatis XML
│   │           ├── UserMapper.xml
│   │           ├── BookmarkMapper.xml
│   │           └── CategoryMapper.xml
│   └── test/
│       └── java/
│           └── com/
│               └── bookmark/
│                   ├── BookmarkApplicationTests.java
│                   └── service/
│                       ├── BookmarkServiceTest.java
│                       └── UserServiceTest.java
├── pom.xml                                              # Maven配置
└── README.md
```

---

## 🔐 安全设计

### 1. 密码加密
使用 **BCrypt** 算法加密用户密码：
```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public void register(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // 保存用户...
    }
}
```

### 2. JWT认证
配置JWT Token验证：
```java
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

### 3. CORS配置
允许前端跨域访问：
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

### 4. 输入验证
使用 Validation 注解验证请求参数：
```java
public class BookmarkRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255")
    private String title;
    
    @NotBlank(message = "URL不能为空")
    @Pattern(regexp = "^https?://.*", message = "URL格式不正确")
    private String url;
}
```

---

## 📦 Maven依赖 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.bookmark</groupId>
    <artifactId>bookmark-backend</artifactId>
    <version>1.0.0</version>
    <name>bookmark-backend</name>
    
    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <jwt.version>0.12.3</jwt.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        
        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.25</version>
        </dependency>
        
        <!-- Knife4j (Swagger) -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
            <version>4.4.0</version>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## ⚙️ 配置文件 (application.yml)

```yaml
spring:
  application:
    name: bookmark-backend
  
  # 数据源配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/bookmark_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    
  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
      timeout: 3000ms
      
  # Jackson配置
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss

# MyBatis Plus配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath*:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: status
      logic-delete-value: 0
      logic-not-delete-value: 1

# JWT配置
jwt:
  secret: your-256-bit-secret-key-change-this-in-production
  expiration: 604800000  # 7天 (毫秒)

# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /api

# Knife4j配置
knife4j:
  enable: true
  setting:
    language: zh_cn
```

---

## 🚀 部署建议

### 1. 开发环境
```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE bookmark_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 执行SQL脚本(创建表)
mysql -u root -p bookmark_db < schema.sql

# 3. 启动Redis
redis-server

# 4. 启动后端
mvn spring-boot:run

# 5. 访问API文档
http://localhost:8080/api/doc.html
```

### 2. 生产环境
- 使用 **Nginx** 作为反向代理
- 使用 **Docker** 容器化部署
- 配置 **HTTPS** 证书
- 使用 **云数据库** (RDS)
- 配置 **Redis集群**

### 3. Docker部署示例
```dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY target/bookmark-backend-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080
```

---

## 📝 开发步骤建议

1. **环境准备**
   - 安装 JDK 17+
   - 安装 Maven
   - 安装 MySQL 8.0+
   - 安装 Redis

2. **数据库初始化**
   - 创建数据库
   - 执行SQL脚本创建表

3. **创建Spring Boot项目**
   - 使用Spring Initializr创建项目
   - 添加依赖到pom.xml

4. **编写核心代码**
   - 实体类 (Entity)
   - Mapper接口
   - Service业务逻辑
   - Controller接口

5. **配置安全认证**
   - JWT工具类
   - Security配置
   - 登录/注册接口

6. **测试接口**
   - 使用Postman测试
   - 查看Swagger文档

7. **前后端联调**
   - 配置CORS
   - 前端axios配置
   - 测试完整流程

---

## 🎯 后续优化建议

1. **性能优化**
   - Redis缓存热点数据
   - 数据库索引优化
   - 分页查询优化

2. **功能扩展**
   - 书签导入/导出 (HTML, Chrome格式)
   - 书签分享功能
   - 标签云展示
   - 全文搜索 (Elasticsearch)

3. **运维监控**
   - 接入日志系统 (ELK)
   - 性能监控 (Prometheus + Grafana)
   - 异常告警

---

## 📞 联系方式

如有问题，请参考文档或联系开发团队。

祝开发顺利！🎉
