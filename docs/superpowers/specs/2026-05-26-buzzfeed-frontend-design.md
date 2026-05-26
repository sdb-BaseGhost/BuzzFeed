# BuzzFeed Frontend 设计文档

## 概述

BuzzFeed 是一个类 Twitter 社交媒体前端应用，使用 Vue 3 技术栈构建，当前阶段使用 mock 数据独立运行，后续对接 Spring Boot 后端 API。

## 技术栈

| 类别 | 选型 | 说明 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | `<script setup>` 语法 |
| 构建 | Vite | 快速 HMR，开箱即用 |
| 样式 | Tailwind CSS | 原子化 CSS，高度自定义 |
| 状态管理 | Pinia | Vue 3 官方推荐，轻量 |
| HTTP | Axios | 拦截器统一处理 JWT、错误 |
| 路由 | Vue Router 4 | 路由守卫处理鉴权 |
| 逻辑复用 | Composables | useFeed, useAuth, usePost 等 |

## 视觉设计

### 布局：经典 Twitter 三栏

```
┌──────────────────────────────────────────────────────────┐
│  ┌──────┐  ┌─────────────────────┐  ┌─────────────────┐  │
│  │      │  │                     │  │                 │  │
│  │ Logo │  │     Feed 流区域      │  │   搜索/推荐     │  │
│  │      │  │                     │  │                 │  │
│  │ 首页  │  │  ┌───────────────┐  │  │  ┌───────────┐  │  │
│  │ 关注  │  │  │   Post Card   │  │  │  │  热门话题  │  │  │
│  │ 搜索  │  │  ├───────────────┤  │  │  ├───────────┤  │  │
│  │ 通知  │  │  │   Post Card   │  │  │  │  推荐用户  │  │  │
│  │ 消息  │  │  ├───────────────┤  │  │  ├───────────┤  │  │
│  │ 个人  │  │  │   Post Card   │  │  │  │           │  │  │
│  │      │  │  └───────────────┘  │  │  └───────────┘  │  │
│  │      │  │                     │  │                 │  │
│  └──────┘  └─────────────────────┘  └─────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

- **左栏**：60px 图标导航栏（Logo、首页、关注、搜索、通知、消息、个人头像）
- **中栏**：Feed 流内容区，最大宽度 600px，居中
- **右栏**：搜索框 + 热门话题 + 推荐用户，宽度约 350px

### 主题：Twitter 深色

| Token | 色值 | 用途 |
|-------|------|------|
| bg-primary | #15202b | 页面主背景 |
| bg-secondary | #192734 | 卡片/输入框背景 |
| bg-hover | #1d2f3f | 悬停状态 |
| border | #38444d | 分割线、边框 |
| text-primary | #e7e9ea | 主文字 |
| text-secondary | #536471 | 次要文字、时间戳 |
| accent | #1d9bf0 | 主题色（按钮、链接、Logo） |
| danger | #f4212e | 删除、取关 |
| success | #00ba7c | 成功状态 |

Tailwind 配置中扩展这些自定义色值到 `theme.extend.colors`。

## 路由规划

| 路径 | 页面 | 鉴权 | 说明 |
|------|------|------|------|
| `/login` | Login | 否 | 登录页 |
| `/register` | Register | 否 | 注册页 |
| `/` | Home | 是 | 首页 Feed（推荐/全局） |
| `/following` | Following | 是 | 关注页 Feed |
| `/explore` | Explore | 是 | 搜索/发现页 |
| `/notifications` | Notifications | 是 | 通知页 |
| `/messages` | Messages | 是 | 消息页 |
| `/profile/:userId` | Profile | 是 | 用户资料页 |
| `/profile/:userId/followers` | Followers | 是 | 粉丝列表 |
| `/profile/:userId/following` | FollowingList | 是 | 关注列表 |
| `/post/:postId` | PostDetail | 是 | 帖子详情 + 评论 |

路由守卫：未登录用户访问需鉴权页面时重定向到 `/login`。已登录用户访问 `/login` 或 `/register` 时重定向到 `/`。

## 目录结构

```
Frontend/
├── src/
│   ├── api/                    # API 接口层
│   │   ├── index.js            # Axios 实例、拦截器、mock 开关
│   │   ├── auth.js             # POST /auth/login, /auth/register
│   │   ├── feed.js             # POST /feed/getFeed
│   │   ├── post.js             # POST /post, 点赞, 评论
│   │   ├── user.js             # 用户信息, 关注/取关
│   │   └── search.js           # 搜索接口
│   ├── assets/                 # 静态资源（图标、图片）
│   ├── components/             # 通用组件
│   │   ├── layout/
│   │   │   ├── AppLayout.vue       # 三栏布局骨架
│   │   │   ├── LeftSidebar.vue     # 左侧导航栏
│   │   │   └── RightSidebar.vue    # 右侧搜索/推荐
│   │   ├── feed/
│   │   │   ├── FeedList.vue        # Feed 流列表（含无限滚动）
│   │   │   └── FeedItem.vue        # 单条 Feed 卡片
│   │   ├── post/
│   │   │   ├── PostComposer.vue    # 发布输入框
│   │   │   ├── PostCard.vue        # 帖子卡片（含点赞/评论操作）
│   │   │   └── MediaUploader.vue   # 图片/视频上传
│   │   ├── user/
│   │   │   ├── UserCard.vue        # 用户信息卡片
│   │   │   ├── UserAvatar.vue      # 头像组件
│   │   │   └── FollowButton.vue    # 关注/取关按钮
│   │   ├── comment/
│   │   │   ├── CommentList.vue     # 评论列表
│   │   │   └── CommentItem.vue     # 单条评论
│   │   └── common/
│   │       ├── BaseButton.vue      # 通用按钮
│   │       ├── BaseInput.vue       # 通用输入框
│   │       ├── BaseModal.vue       # 弹窗
│   │       ├── LoadingSpinner.vue  # 加载动画
│   │       └── InfiniteScroll.vue  # 无限滚动指令/组件
│   ├── composables/            # 逻辑复用
│   │   ├── useAuth.js          # 登录/注册/登出/token管理
│   │   ├── useFeed.js          # Feed 流加载、分页、无限滚动
│   │   ├── usePost.js          # 发布、点赞、评论
│   │   ├── useUser.js          # 用户信息、关注/取关
│   │   └── useSearch.js        # 搜索逻辑
│   ├── mock/                   # Mock 数据
│   │   ├── index.js            # mock 总开关
│   │   ├── auth.js             # 登录/注册 mock
│   │   ├── feed.js             # Feed 数据 mock
│   │   ├── users.js            # 用户数据 mock
│   │   └── posts.js            # 帖子数据 mock
│   ├── router/
│   │   └── index.js            # 路由配置 + 守卫
│   ├── stores/                 # Pinia stores
│   │   ├── auth.js             # 认证状态（token, currentUser）
│   │   ├── feed.js             # Feed 流数据
│   │   └── ui.js               # UI 状态（弹窗、侧边栏）
│   ├── views/                  # 页面组件
│   │   ├── Login.vue
│   │   ├── Register.vue
│   │   ├── Home.vue
│   │   ├── Following.vue
│   │   ├── Explore.vue
│   │   ├── Notifications.vue
│   │   ├── Messages.vue
│   │   ├── Profile.vue
│   │   ├── Followers.vue
│   │   ├── FollowingList.vue
│   │   └── PostDetail.vue
│   ├── App.vue
│   ├── main.js
│   └── style.css               # Tailwind 入口
├── public/
├── index.html
├── tailwind.config.js
├── vite.config.js
├── postcss.config.js
└── package.json
```

## Pinia Stores 设计

### auth store

```js
// stores/auth.js
{
  state: {
    token: null,          // JWT token
    currentUser: null,    // 当前用户信息 { userId, username, avatar, ... }
  },
  getters: {
    isLoggedIn,           // !!token
  },
  actions: {
    login(username, password),
    register(username, password, email),
    logout(),
    fetchCurrentUser(),
  }
}
```

### feed store

```js
// stores/feed.js
{
  state: {
    posts: [],            // 首页 feed 列表
    followingPosts: [],   // 关注页 feed 列表
    cursor: null,         // 分页游标
    loading: false,
    hasMore: true,
  },
  actions: {
    fetchFeed(type),      // type: 'recommend' | 'following'
    loadMore(type),
    addPost(post),        // 发布后插入到列表顶部
    toggleLike(postId),
    addComment(postId, comment),
  }
}
```

### ui store

```js
// stores/ui.js
{
  state: {
    showPostComposer: false,  // 发布弹窗
    showLoginModal: false,
  }
}
```

## API 层与 Mock 策略

### Axios 实例配置

`api/index.js` 创建 Axios 实例：
- `baseURL`：开发环境指向 mock，生产环境指向后端 `http://localhost:8000`
- 请求拦截器：自动附加 `Authorization: Bearer <token>` header
- 响应拦截器：统一处理 `Result` 格式（`code`, `msg`, `data`），401 时清除 token 并跳转登录

### Mock 开关

```js
// api/index.js
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

// 每个 api 模块根据 USE_MOCK 决定调用真实接口还是 mock
export async function getFeed(params) {
  if (USE_MOCK) return mockFeed(params)
  return api.post('/feed/getFeed', params)
}
```

- `VITE_USE_MOCK=true`（默认）：使用 mock 数据
- `VITE_USE_MOCK=false`：对接真实后端

只需修改 `.env` 文件即可切换，业务代码零改动。

### Mock 数据结构

mock 数据尽量模拟后端 `Result` 格式：

```js
{
  code: 200,
  msg: "success",
  data: { ... }
}
```

## 核心组件设计

### FeedList + 无限滚动

- 使用 `IntersectionObserver` 实现无限滚动
- 基于游标分页（cursor-based），每次请求传入上一页最后一条的 `publishTime` 作为游标
- 滚动到底部时自动触发 `loadMore()`
- 显示 loading 骨架屏
- `hasMore: false` 时显示"没有更多了"

### PostCard

显示内容：
- 用户头像、昵称、@用户名、发布时间
- 文字内容
- 图片（网格展示，最多 4 张）/ 视频播放器
- 操作栏：评论数、点赞数（点击可操作）
- 点击卡片跳转到 PostDetail

### PostComposer

- 文字输入框（textarea，自适应高度）
- 图片/视频上传按钮
- 字数统计
- 发布按钮
- 首页 Feed 顶部内嵌输入框，点击展开完整编辑器（非弹窗）

### Profile 页面

- 顶部封面图 + 头像
- 用户名、简介、关注/粉丝数、帖子数
- 关注/取关按钮（非本人时显示）
- Tab 切换：帖子 | 媒体 | 点赞
- 帖子列表（复用 FeedList）

### 登录/注册

- 简洁表单：用户名 + 密码（+ 邮箱 for 注册）
- 表单验证
- 错误提示
- 注册成功后自动跳转登录

## 实现顺序建议

分 4 个阶段，每阶段可独立验证：

### Phase 1：骨架 + 鉴权
1. 项目初始化（Vite + Vue 3 + Tailwind + Pinia + Vue Router）
2. 三栏布局骨架（AppLayout, LeftSidebar, RightSidebar）
3. 路由配置 + 路由守卫
4. 登录/注册页面 + mock
5. auth store + useAuth composable

### Phase 2：Feed 核心
1. Feed store + useFeed composable
2. FeedList + FeedItem 组件
3. 无限滚动
4. 首页 Feed（/）
5. 关注页 Feed（/following）

### Phase 3：互动功能
1. PostComposer + 发布功能
2. PostCard + 点赞
3. PostDetail 页面 + 评论列表
4. MediaUploader（图片/视频上传预览）

### Phase 4：社交功能
1. Profile 页面 + 关注/粉丝列表
2. FollowButton + 关注/取关
3. Explore 搜索页面
4. Notifications 通知页面
5. Messages 消息页面

## 后端对接约定

后端 API 基础路径：`http://localhost:8000`

已知接口：
- `POST /post` — 发布内容，Body: `{ shortText, longText, photo, video, music }`
- `POST /feed/getFeed` — 获取 feed，Body: `{ userId, cursor, type }`

待补充接口（前端 mock，后端后续实现）：
- `POST /auth/login` — 登录
- `POST /auth/register` — 注册
- `POST /post/:id/like` — 点赞
- `POST /post/:id/comment` — 评论
- `GET /user/:id` — 用户信息
- `POST /user/:id/follow` — 关注
- `DELETE /user/:id/follow` — 取关
- `GET /search` — 搜索
- `GET /notifications` — 通知列表
- `GET /messages` — 消息列表

所有接口统一返回 `{ code, msg, data }` 格式。
