# 登录注册模块

## 概述

基于 Spring Security + JWT 的无状态认证，支持注册、登录、登出、获取当前用户。

## 涉及文件

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/AuthController.java` | 注册/登录/登出/me 四个端点 |
| Service | `service/AuthService.java` + `impl/AuthServiceImpl.java` | 密码加密、JWT 生成、用户查询、Redis 活跃标记 |
| Mapper | `mapper/UserMapper.java` + `.xml` | 用户 CRUD |
| 工具 | `utils/JwtUtil.java` | JWT 签发/解析 (HS256, 24h 有效期) |
| 工具 | `utils/JwtInterceptor.java` | 请求拦截，解析 token 写入 UserContext |
| 工具 | `utils/UserContext.java` | ThreadLocal 存储当前 userId |
| 配置 | `config/SecurityConfig.java` | BCrypt PasswordEncoder + CORS + 无状态 Session |
| 配置 | `config/WebConfig.java` | 注册 JwtInterceptor，排除 /api/auth/login 和 /register |
| 实体 | `entity/User.java` | userId / username / passwordHash / email / displayName / is_active 等 |

## API 设计

```
POST /api/auth/register   Body: {username, password, email, displayName}
POST /api/auth/login      Body: {username, password}
POST /api/auth/logout
GET  /api/auth/me          Header: Authorization: Bearer <token>
```

## 认证流程

```
请求 → JwtInterceptor.preHandle()
  ├─ 检查 Authorization header
  ├─ JwtUtil.parseToken(token) → userId
  ├─ UserContext.setUserId(userId)   // 写入 ThreadLocal
  └─ 业务代码通过 UserContext.getUserId() 获取当前用户

请求结束 → JwtInterceptor.afterCompletion()
  └─ UserContext.remove()            // 清理 ThreadLocal 防内存泄漏
```

## 数据库表

`user` 表，字段: user_id(PK) / username(UK) / password_hash / email / display_name / bio / avatar / is_active / follower_number / follows_number / post_count

## 设计决策

- 密码用 BCrypt 哈希，不存明文
- JWT HS256，有效期 24 小时
- 登录时写 `active:user:{id}` (TTL 7天)，Fanout 用此判断是否推给该用户
- UserContext 用 ThreadLocal，Service 层不用每方法传 userId
