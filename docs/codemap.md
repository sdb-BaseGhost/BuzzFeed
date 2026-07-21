# BuzzFeed 项目结构 (Code Map)

> 最后更新: 2026-07-21

## 后端目录树

```
src/main/java/org/sdb/buzzfeed/
├── BuzzFeedApplication.java              # 启动类
│
├── config/                                # ── 配置层 ──
│   ├── SecurityConfig.java               # Spring Security + CORS (localhost:5173/5174)
│   ├── KafkaConfig.java                  # Kafka 容器工厂: Dispatcher(concurrency=1) + Executor(concurrency=可配置)
│   ├── MinioConfig.java                  # MinIO 客户端 Bean
│   ├── WebConfig.java                    # Web 拦截器注册 (JWT)
│   └── GlobalExceptionHandler.java       # 全局异常处理 → @RestControllerAdvice
│
├── controller/                            # ── 控制器层 ──
│   ├── AuthController.java               # POST /api/auth/register,login,logout  GET /api/auth/me
│   ├── FeedController.java               # POST /feed/getFeed (推拉结合 Feed 流)
│   ├── PostController.java               # POST /post (发帖)  GET /api/post/user/{userId}
│   ├── FollowController.java             # POST/DELETE /api/follow  GET /api/follow/{following,followers,status}
│   ├── UserController.java               # GET /api/user/{userId}  GET /api/user/{userId}/following,followers
│   ├── UploadController.java             # POST /upload/image  /upload/video (MinIO 上传)
│   ├── FileProxyController.java          # GET /files/** → MinIO 文件代理 (解决前端跨域/鉴权)
│   ├── SearchController.java             # GET /api/search?q= (用户搜索)
│   └── KafkaController.java              # Kafka 测试端点
│
├── entity/                                # ── 实体层 ──
│   ├── Content.java                      # 内容主实体 → item_info 表
│   ├── ContentImage.java                 # 内容图片 → image_info 表
│   ├── ContentVideo.java                 # 内容视频 → video_info 表
│   ├── Feed.java                         # Feed 请求参数 (type/lastTime/contentId/userId)
│   ├── User.java                         # 用户实体 → user 表
│   ├── FollowRelation.java               # 关注关系 (following/follower 表共用结构)
│   ├── FanoutMessage.java                # Fan-out Kafka 消息体 (contentId/creatorId/publishTime)
│   ├── FanoutSubTask.java                # 分片子任务 (contentId/taskId/totalShards/publishTime/fanIds)
│   ├── Result.java                       # 统一响应封装 { code, msg, data }
│   ├── dto/
│   │   ├── CreatePostDTO.java            # 发帖请求参数 (@Valid 校验: contentType/title/visibility)
│   │   ├── LoginDTO.java                 # 登录请求参数
│   │   └── RegisterDTO.java              # 注册请求参数
│   └── vo/
│       ├── CreatePostVO.java             # 发帖返回 { itemId }
│       ├── FeedItemVO.java               # Feed 流单条内容 (内容+用户+媒体)
│       ├── LoginVO.java                  # 登录返回
│       ├── RegisterVO.java               # 注册返回
│       ├── SearchUserVO.java             # 搜索用户结果
│       └── UploadVO.java                 # 文件上传返回 (url/objectName/coverUrl)
│
├── mapper/                                # ── 数据访问层 ──
│   ├── UserMapper.java + .xml            # 用户 CRUD / 搜索 / 活跃查询
│   ├── FollowMapper.java + .xml          # 关注关系 CRUD (following/follower 双表)
│   ├── PostMapper.java + .xml            # 内容写入/状态更新
│   ├── ContentMapper.java                # 内容查询
│   ├── ContentImageMapper.java + .xml    # 图片关联查询
│   ├── ContentVideoMapper.java + .xml    # 视频关联查询
│   ├── InboxMapper.java + .xml           # 收件箱 CRUD (推模式)
│   └── OutboxMapper.java + .xml          # 发件箱查询 (拉模式)
│
├── service/                               # ── 业务逻辑层 ──
│   ├── AuthService.java                  # 认证接口
│   ├── FeedService.java                  # Feed 流接口
│   ├── PostService.java                  # 发帖接口
│   ├── FollowService.java                # 关注/取关/关系查询接口
│   ├── MinioService.java                 # MinIO 文件上传 (含视频转码+封面截帧)
│   ├── ContentCacheService.java          # 内容 Redis Hash 缓存业务层读写
│   ├── VideoTranscodeService.java        # FFmpeg 视频转码 (H.265→H.264) + 封面截帧
│   ├── KafkaConsumerService.java         # Kafka 消费者 (内容审核)
│   ├── FanoutDispatcher.java             # ⭐ Kafka 消费者: 拉粉丝 + 大V判断 + 动态分片 → 发子任务
│   ├── FanoutExecutor.java               # ⭐ Kafka 消费者: 并行消费子任务 → 批量写 inbox
│   ├── ContentCacheConsumer.java         # ⭐ Canal binlog 消费者: 维护 content Hash + outbox 缓存
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── FeedServiceImpl.java          # ⭐ 推拉结合 Feed 流
│       ├── PostServiceImpl.java          # ⭐ 发帖 + MinIO + Kafka Fan-out
│       └── FollowServiceImpl.java        # ⭐ 关注逻辑 (DB + Redis ZSET 双写)
│
└── utils/                                 # ── 工具层 ──
    ├── JwtUtil.java                      # JWT 生成/解析
    ├── JwtInterceptor.java               # JWT 请求拦截器
    ├── UserContext.java                  # ThreadLocal 用户上下文
    └── RedisFeedHelper.java             # ⭐ Feed 流 Redis 操作封装 (inbox/outbox ZSET + content Hash + 游标分页)

src/main/resources/
├── application.yml                       # 全局配置 (MySQL/Redis/Kafka/MinIO/Canal)
└── mapper/                               # MyBatis XML (与 Java mapper 一一对应)
    ├── UserMapper.xml
    ├── FollowMapper.xml
    ├── PostMapper.xml
    ├── ContentImageMapper.xml
    ├── ContentVideoMapper.xml
    ├── InboxMapper.xml
    └── OutboxMapper.xml

docker/
└── canal/                                # Canal 基础设施配置
```

---

## 前端目录树

```
Frontend/src/
├── main.js                               # 入口：挂载 Pinia + Router + App
├── App.vue                               # 根组件
├── style.css                             # 全局样式 (Tailwind)
│
├── api/                                  # ── 后端接口调用层 ──
│   ├── index.js                          # axios 实例 + 请求/响应拦截器 + USE_MOCK 开关
│   ├── auth.js                           # login / register / getCurrentUser
│   ├── feed.js                           # getFeed
│   ├── post.js                           # createPost / toggleLike / getPostDetail / getComments / addComment
│   ├── user.js                           # getUser / getFollowing / getFollowers
│   ├── follow.js                         # followUser / unfollowUser / getFollowingList / getFollowerList / getFollowStatus
│   ├── search.js                         # searchUsers
│   └── upload.js                         # uploadImage / uploadVideo
│
├── stores/                               # ── Pinia 全局状态 (Composition API) ──
│   ├── auth.js                           # token / currentUser / login / logout
│   ├── feed.js                           # posts / cursor / hasMore / fetchFeed / loadMore / toggleLike
│   └── ui.js                             # UI 状态
│
├── composables/                          # ── 组合式函数 (View ↔ Store 桥梁) ──
│   ├── useAuth.js                        # login / register / logout + loading/error
│   ├── useFeed.js                        # loadFeed / loadMore + loading
│   └── usePost.js                        # publishPost + loading/error
│
├── views/                                # ── 页面组件 (路由级) ──
│   ├── Home.vue                          # 首页 (推荐/关注 Tab)        → /
│   ├── Login.vue                         # 登录                        → /login
│   ├── Register.vue                      # 注册                        → /register
│   ├── Compose.vue                       # 发帖 (图文/视频)            → /compose
│   ├── PostDetail.vue                    # 帖子详情 + 评论              → /post/:postId
│   ├── Profile.vue                       # 个人主页                     → /profile/:userId
│   ├── Explore.vue                       # 发现                        → /explore
│   ├── Following.vue                     # 关注 Feed                   → /following
│   ├── Notifications.vue                 # 通知                        → /notifications
│   ├── Messages.vue                      # 私信                        → /messages
│   ├── Followers.vue                     # 粉丝列表                    → /profile/:userId/followers
│   └── FollowingList.vue                 # 关注列表                    → /profile/:userId/following
│
├── components/                           # ── 可复用 UI 组件 (按功能模块分子目录) ──
│   ├── feed/
│   │   ├── FeedList.vue                  # Feed 列表 (无限滚动)
│   │   └── FeedItem.vue                  # 单条 Feed 卡片
│   ├── post/
│   │   └── PostComposer.vue              # 发帖编辑器组件
│   ├── comment/
│   │   ├── CommentList.vue               # 评论列表
│   │   └── CommentItem.vue               # 单条评论
│   ├── user/
│   │   ├── UserAvatar.vue                # 用户头像
│   │   ├── FollowButton.vue              # 关注/取关按钮
│   │   └── UserProfileModal.vue          # 用户资料弹窗 (Profile 页点击头像)
│   └── layout/
│       ├── AppLayout.vue                 # 全局布局 (三栏)
│       ├── LeftSidebar.vue               # 左侧导航
│       └── RightSidebar.vue              # 右侧栏
│
├── mock/                                 # ── Mock 数据 ──
│   ├── index.js                          # delay / generateId / mockResult 通用工具
│   ├── auth.js                           # 登录注册 mock
│   ├── feed.js                           # Feed 流 mock
│   ├── posts.js                          # 帖子 mock
│   └── users.js                          # 用户 mock
│
└── router/
    └── index.js                          # 路由定义 + beforeEach 守卫 (requiresAuth / requiresGuest)
```

---

## 核心文件导航

| 你要做的事 | 后端 | 前端 |
|-----------|------|------|
| **Feed 流推拉结合** | `service/impl/FeedServiceImpl.java` | `api/feed.js` + `stores/feed.js` + `composables/useFeed.js` |
| **Fan-out 推送** | `service/FanoutDispatcher.java` + `FanoutExecutor.java` (Kafka 分片→inbox) | — (异步，前端无感知) |
| **Canal 增量同步** | `service/ContentCacheConsumer.java` (binlog→Redis Hash+outbox) | — (基础设施层) |
| **Redis 多级缓存** | `utils/RedisFeedHelper.java` + `service/ContentCacheService.java` | — |
| **发帖链路** | `service/impl/PostServiceImpl.java` (写DB+Kafka触发Fan-out) | `api/post.js` + `views/Compose.vue` |
| **关注/粉丝** | `service/impl/FollowServiceImpl.java` (DB+Redis ZSET双写) | `api/follow.js` + `components/user/FollowButton.vue` |
| **文件上传** | `controller/UploadController.java` → `MinioService` → `VideoTranscodeService` | `api/upload.js` |
| **文件访问** | `controller/FileProxyController.java` (MinIO代理) | — |
| **用户搜索** | `controller/SearchController.java` | `api/search.js` |
| **内容审核** | `service/KafkaConsumerService.java` | — (异步，前端无感知) |
| **认证流程** | `utils/JwtUtil.java` + `JwtInterceptor.java` | `api/index.js`(拦截器) + `stores/auth.js` + `composables/useAuth.js` |
| **全局异常** | `config/GlobalExceptionHandler.java` | — |
| **路由守卫** | — | `router/index.js` (meta.requiresAuth) |
| **数据库表** | `docs/table.md` | — |
| **Mock 机制** | — | `api/index.js` (USE_MOCK) + `mock/` 目录 |
