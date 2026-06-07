# BuzzFeed — 亿级流量社交 Feed 流系统

## 项目概述

基于《亿级流量系统架构设计与实战》(李琛轩) 构建的社交 Feed 流系统，对标微博/Twitter 的信息流架构。
**秋招项目**，核心亮点：推拉结合 Feed 流、Kafka 异步解耦、多级缓存、Canal 增量同步。

---

## 技术栈

### 后端
Java 17 · Spring Boot 3.5.6 · MyBatis · MySQL 8.0 · Redis + Caffeine · Kafka · MinIO · Canal · JWT

### 前端
Vue 3 (Composition API `<script setup>`) · Vite · Pinia · Vue Router · Axios · Tailwind CSS

---

## 后端分层规范

| 层 | 职责 | 禁止 |
|----|------|------|
| **Controller** | 参数校验 + 调用 Service + 返回 `Result` | 业务逻辑 |
| **Service** | 业务逻辑 | 直接操作 HttpServletRequest |
| **Mapper** | 数据库访问 | 业务判断 |

---

## 前端分层规范

| 层 | 目录 | 职责 | 规则 |
|----|------|------|------|
| **api/** | `src/api/{module}.js` | 后端接口调用，每个功能模块一个文件 | 每个函数必须处理 `USE_MOCK` 分支；统一走 `api` 实例 (自动带 token) |
| **stores/** | `src/stores/{module}.js` | Pinia store，管理全局状态 (auth/feed/ui) | Composition API (`defineStore(() => {...})`)；只存状态和原子操作 |
| **composables/** | `src/composables/use{Module}.js` | 组合式函数，封装 loading/error + store 调用 | View 层通过 composable 访问 store，不要直接调 store |
| **views/** | `src/views/{Name}.vue` | 页面级组件 | 只做布局 + 组合子组件 + 调用 composable |
| **components/** | `src/components/{module}/{Name}.vue` | 可复用 UI 组件 | 按功能模块分子目录 (feed/post/comment/user/layout) |
| **mock/** | `src/mock/{module}.js` | Mock 数据 | 每个模块独立 mock 文件 |

### 前端分层调用关系

```
View (views/)
  → Composable (composables/useXxx.js)  // 管理 loading/error
    → Store (stores/xxx.js)             // 管理状态
      → API (api/xxx.js)                // 管理请求
        → Mock (mock/xxx.js)            // 本地 mock 数据
```

---

## 前后端协作规范 ⭐

### API 契约

后端返回格式统一为 `Result`：
```json
{ "code": 200, "msg": "success", "data": {...} }
```
- 成功: `code: 200`
- 失败: `code: -1` (默认) 或其他业务错误码
- 前端 axios 拦截器已约定 `code !== 200` 视为失败

### 前后端字段命名对照

| 后端 (Java/DB) | 前端 (JS) | 说明 |
|----------------|-----------|------|
| `item_id` / `itemId` | `postId` | 前端统一叫 postId |
| `creator_id` / `creatorId` | `userId` | 前端统一叫 userId |
| `publish_time` | `publishTime` | ISO 8601 字符串 |
| `like_count` | `likeCount` | 数字 |
| `comment_count` | `commentCount` | 数字 |
| `is_active` | - | 后端内部使用，不暴露给前端 |

### 改功能时的协作流程

```
1. 先在 docs/{功能}/CLAUDE.md 确认 API 设计
2. 后端: Controller → Service → Mapper → 测试通过
3. 前端: api/{module}.js 新增接口函数 (含 mock 分支)
4. 前端: stores / composables 按需扩展
5. 前端: views / components 实现 UI
6. 联调: 关闭 mock (VITE_USE_MOCK=false)，对接真实接口
```

### 环境切换

前端通过 `.env` 中 `VITE_USE_MOCK` 控制：
- `true` → 走 mock 数据 (独立开发前端 UI)
- `false` → 走真实后端接口 (联调/生产)

### 前端端口

| 环境 | 前端 | 后端 |
|------|------|------|
| 开发 | localhost:5173 | localhost:8000 |
| 代理 | Vite proxy 或直接 axios baseURL | — |

### CORS

后端 `SecurityConfig.java` 已允许 `localhost:5173` 和 `localhost:5174`。
新增环境需同步修改 `corsConfigurationSource()`。

---

## API 路由总表

| 方法 | 路径 | 前端文件 | 后端文件 | 状态 |
|------|------|---------|---------|------|
| POST | `/api/auth/register` | `api/auth.js` | `AuthController.java` | ✅ 已完成 |
| POST | `/api/auth/login` | `api/auth.js` | `AuthController.java` | ✅ 已完成 |
| POST | `/api/auth/logout` | `api/auth.js` | `AuthController.java` | ✅ 已完成 |
| GET | `/api/auth/me` | `api/auth.js` | `AuthController.java` | ✅ 已完成 |
| POST | `/post` | `api/post.js` | `PostController.java` | ✅ 已完成 |
| POST | `/feed/getFeed` | `api/feed.js` | `FeedController.java` | ✅ 已完成 |
| GET | `/user/{id}` | `api/user.js` | 待开发 | 🔲 mock only |
| POST | `/user/{id}/follow` | `api/user.js` | 待开发 | 🔲 mock only |
| DELETE | `/user/{id}/follow` | `api/user.js` | 待开发 | 🔲 mock only |
| POST | `/post/{id}/like` | `api/post.js` | 待开发 | 🔲 mock only |
| GET | `/post/{id}` | `api/post.js` | 待开发 | 🔲 mock only |
| GET | `/post/{id}/comments` | `api/post.js` | 待开发 | 🔲 mock only |
| POST | `/post/{id}/comment` | `api/post.js` | 待开发 | 🔲 mock only |

---

## 项目结构 & 功能文档

- 代码目录树 → [`docs/codemap.md`](docs/codemap.md)
- 数据库 DDL → [`docs/table.md`](docs/table.md)

各功能模块详细设计见 `docs/` 下对应子目录：

| 功能 | 文档路径 |
|------|---------|
| 登录注册 | [`docs/auth/CLAUDE.md`](docs/auth/CLAUDE.md) |
| 发帖 (图文/视频/文本) | [`docs/post/CLAUDE.md`](docs/post/CLAUDE.md) |
| Feed 流 | [`docs/feed/CLAUDE.md`](docs/feed/CLAUDE.md) |
| 评论 | [`docs/comment/CLAUDE.md`](docs/comment/CLAUDE.md) |
| 点赞 | [`docs/like/CLAUDE.md`](docs/like/CLAUDE.md) |

---

## 本地开发环境

```
MySQL 8.0  → localhost:3306  (buzzfeed / 1234)
Redis      → localhost:6379  (密码: 123456)
Kafka      → localhost:9092
MinIO      → localhost:9000  (minioadmin)
```

启动: `mvn spring-boot:run` (8000) + `cd Frontend && npm run dev` (5173)

---

## 秋招面试要点

1. **推拉结合 Feed 流**: 大V拉 + 普通推，多路归并 K Sorted Lists
2. **Kafka 异步解耦**: 发帖 → 审核 → Fan-out，全链路异步
3. **多级缓存**: Caffeine (L1) + Redis (L2) + MySQL (L3)
4. **游标分页**: 基于 (time, id) 避免深分页
5. **Canal 增量同步**: MySQL binlog → Redis 缓存一致性
6. **Redis ZSET**: 收件箱/发件箱排序 + Score 复合键
