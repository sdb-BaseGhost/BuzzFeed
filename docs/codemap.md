# BuzzFeed 项目结构 (Code Map)

## 后端目录树

```
src/main/java/org/sdb/buzzfeed/
├── BuzzFeedApplication.java          # 启动类
├── config/
│   ├── SecurityConfig.java           # Spring Security + CORS 配置
│   ├── MinioConfig.java              # MinIO 客户端 Bean
│   └── WebConfig.java                # Web 拦截器注册
├── controller/
│   ├── AuthController.java           # 注册/登录/登出/me
│   ├── FeedController.java           # Feed 流读取 (上拉/下滑)
│   ├── PostController.java           # 内容发布 (图文/视频)
│   └── KafkaController.java          # Kafka 测试端点
├── entity/
│   ├── Content.java                  # 内容主实体 → item_info 表
│   ├── ContentImage.java             # 内容图片 → image_info 表
│   ├── ContentVideo.java             # 内容视频 → video_info 表
│   ├── Feed.java                     # Feed 请求参数 (type/lastTime/contentId/userId)
│   ├── User.java                     # 用户实体 → user 表
│   ├── UserFollow.java               # 关注关系 → user_follow 表
│   └── Result.java                   # 统一响应封装 { code, msg, data }
├── mapper/
│   ├── userMapper.java + .xml        # 用户/关注/大V/活跃查询
│   ├── postMapper.java + .xml        # 内容写入/状态更新
│   ├── contentMapper.java            # 内容查询
│   ├── inboxMapper.java + .xml       # 收件箱 CRUD (推模式)
│   ├── outboxMapper.java + .xml      # 发件箱查询 (拉模式)
│   ├── ContentImageMapper.java + .xml
│   └── ContentVideoMapper.java + .xml
├── service/
│   ├── AuthService.java              # 认证接口
│   ├── FeedService.java              # Feed 流接口
│   ├── PostService.java              # 发帖接口
│   ├── MinioService.java             # MinIO 文件上传
│   ├── KafkaProducerService.java     # Kafka 生产者
│   ├── KafkaConsumerService.java     # Kafka 消费者 (审核 + Fan-out)
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── FeedServiceImpl.java      # ⭐ 推拉结合 Feed 流
│       └── PostServiceImpl.java      # ⭐ 发帖 + MinIO + Kafka
├── utils/
│   ├── JwtUtil.java                  # JWT 生成/解析
│   ├── JwtInterceptor.java           # JWT 请求拦截器
│   └── UserContext.java              # ThreadLocal 用户上下文
└── util/
    └── util.java

src/main/resources/
├── application.yml                   # 全局配置
└── mapper/                           # MyBatis XML (与 Java mapper 一一对应)
```

---

## 前端目录树

```
Frontend/src/
├── main.js                           # 入口：挂载 Pinia + Router + App
├── App.vue                           # 根组件
├── style.css                         # 全局样式 (Tailwind)
│
├── api/                              # 后端接口调用层
│   ├── index.js                      # axios 实例 + 请求/响应拦截器 + USE_MOCK 开关
│   ├── auth.js                       # login / register / getCurrentUser
│   ├── feed.js                       # getFeed
│   ├── post.js                       # createPost / toggleLike / getPostDetail / getComments / addComment
│   ├── user.js                       # getUser / followUser / unfollowUser
│   ├── search.js                     # 搜索 (待开发)
│   └── upload.js                     # 上传 (待开发)
│
├── stores/                           # Pinia 全局状态 (Composition API)
│   ├── auth.js                       # token / currentUser / login / logout
│   ├── feed.js                       # posts / cursor / hasMore / fetchFeed / loadMore / toggleLike
│   └── ui.js                         # UI 状态
│
├── composables/                      # 组合式函数 (View ↔ Store 桥梁)
│   ├── useAuth.js                    # login / register / logout + loading/error
│   ├── useFeed.js                    # loadFeed / loadMore + loading
│   └── usePost.js                    # publishPost + loading/error
│
├── views/                            # 页面组件 (路由级)
│   ├── Home.vue                      # 首页 (推荐/关注 Tab)
│   ├── Login.vue                     # 登录
│   ├── Register.vue                  # 注册
│   ├── Compose.vue                   # 发帖 (图文/视频)
│   ├── PostDetail.vue                # 帖子详情 + 评论
│   ├── Profile.vue                   # 个人主页
│   ├── Explore.vue                   # 发现
│   ├── Notifications.vue             # 通知
│   ├── Messages.vue                  # 私信
│   ├── Followers.vue                 # 粉丝列表
│   ├── Following.vue                 # 关注 Feed
│   └── FollowingList.vue             # 关注列表
│
├── components/                       # 可复用 UI 组件 (按功能模块分子目录)
│   ├── feed/
│   │   ├── FeedList.vue              # Feed 列表 (无限滚动)
│   │   └── FeedItem.vue              # 单条 Feed 卡片
│   ├── post/
│   │   └── PostComposer.vue          # 发帖编辑器组件
│   ├── comment/
│   │   ├── CommentList.vue           # 评论列表
│   │   └── CommentItem.vue           # 单条评论
│   ├── user/
│   │   ├── UserAvatar.vue            # 用户头像
│   │   └── FollowButton.vue          # 关注/取关按钮
│   └── layout/
│       ├── AppLayout.vue             # 全局布局 (三栏)
│       ├── LeftSidebar.vue           # 左侧导航
│       └── RightSidebar.vue          # 右侧栏
│
├── mock/                             # Mock 数据 (开发阶段独立于后端)
│   ├── index.js                      # delay / generateId / mockResult 通用工具
│   ├── auth.js                       # 登录注册 mock
│   ├── feed.js                       # Feed 流 mock
│   ├── posts.js                      # 帖子 mock
│   └── users.js                      # 用户 mock
│
└── router/
    └── index.js                      # 路由定义 + beforeEach 守卫 (requiresAuth / requiresGuest)
```

---

## 核心文件导航

| 你要做的事 | 后端 | 前端 |
|-----------|------|------|
| 理解 Feed 流推拉结合 | `service/impl/FeedServiceImpl.java` | `api/feed.js` + `stores/feed.js` + `composables/useFeed.js` |
| 理解发帖链路 | `service/impl/PostServiceImpl.java` | `api/post.js` + `views/Compose.vue` |
| 理解 Kafka 审核 | `service/KafkaConsumerService.java` | — (异步，前端无感知) |
| 理解认证流程 | `utils/JwtUtil.java` + `JwtInterceptor.java` | `api/index.js`(拦截器) + `stores/auth.js` + `composables/useAuth.js` |
| 理解路由守卫 | — | `router/index.js` (meta.requiresAuth) |
| 理解数据库表 | `docs/table.md` | — |
| 理解 Mock 机制 | — | `api/index.js` (USE_MOCK) + `mock/` 目录 |
