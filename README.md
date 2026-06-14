# BuzzFeed 项目手册

> 这份文档的目的是让你随时能搞清楚：这个项目做了什么、代码在哪、为什么这么做。
> 不是面试八股，是你自己的项目地图。

---

## 技术栈速查

| 层级 | 选型 |
|------|------|
| 后端 | Spring Boot 3.5.6 + Java 17 + MyBatis |
| 前端 | Vue 3 + Pinia + Vue Router + Tailwind CSS + Vite 5 |
| 数据库 | MySQL（buzzfeed 库，端口 3306） |
| 缓存 | Redis（端口 6379） |
| 消息队列 | Kafka（端口 9092） |
| 文件存储 | MinIO（端口 9000） |
| 视频处理 | FFmpeg（转码 + 截帧） |
| 认证 | JWT + Spring Security |

---

## 项目做了什么（用户视角）

1. **注册/登录**：用户名注册，BCrypt 加密密码，登录后发 JWT Token，前端存 localStorage
2. **发布内容**：支持三种类型 —— 纯文本、图文（最多3张图）、视频（含自动截封面）
3. **Feed 信息流**：首页滚动加载关注者的内容，支持下拉刷新和上滑加载更多
4. **关注/粉丝**：关注、取关、查看关注列表和粉丝列表、互相关注判断
5. **文件上传**：图片和视频上传到 MinIO，视频上传时自动检测 H.265 编码并转为 H.264
6. **用户资料**：查看别人的主页、关注/粉丝数、帖子列表

---

## 功能详解 + 代码在哪

### 1. 认证（注册/登录/JWT）

**做了什么**：用户注册（用户名不能重复），登录后后端返回 JWT Token，前端存 localStorage 里。每次请求自动带上 Token，后端拦截器解析出是谁。登录时还会往 Redis 写一个活跃标记，后面 Feed 流要用。

#### 请求链路：用户登录

```
前端 POST /api/auth/login { username, password }
  │
  ▼
AuthController.login()
  │
  ▼
AuthServiceImpl.login()
  │
  ├─ 1. MySQL: userMapper.selectByUsername(username)
  │     → 查 user 表，拿 password_hash、is_active 等字段
  │
  ├─ 2. BCrypt.matches(输入密码, password_hash) → 密码校验
  │
  ├─ 3. 检查 is_active 字段（账号是否被禁用）
  │
  ├─ 4. Redis: SET active:user:{userId} = "1", TTL=7天
  │     → 写活跃标记，后面 Fanout 推送时用来判断是否推给该用户
  │
  └─ 5. JwtUtil.generateToken(userId, username) → 生成 HS256 JWT（有效期 24h）
        → 返回 { token, userId, username, displayName, avatar }
```

**登录后，每个后续请求都会经过拦截器**：
```
前端请求 Header: Authorization: Bearer <token>
  │
  ▼
JwtInterceptor.preHandle()
  ├─ 解析 JWT → 拿到 userId、username
  └─ 存入 UserContext（ThreadLocal）

Controller / Service
  └─ 任意位置调用 UserContext.getUserId() 获取当前用户

请求结束
  └─ JwtInterceptor.afterCompletion()
       └─ UserContext.clear()  ← 防 ThreadLocal 内存泄漏
```

**代码位置**：

| 什么 | 在哪 |
|------|------|
| 后端接口 | controller/AuthController.java |
| 业务逻辑 | service/impl/AuthServiceImpl.java |
| JWT 生成解析 | utils/JwtUtil.java |
| Token 拦截器 | utils/JwtInterceptor.java |
| 当前用户持有 | utils/UserContext.java（ThreadLocal） |
| 前端登录页 | views/Login.vue |
| 前端注册页 | views/Register.vue |
| auth 状态管理 | stores/auth.js |

**设计决策**：
- 密码用 BCrypt 哈希，不存明文
- JWT HS256，有效期 24 小时
- `active:user:{id}` TTL 7 天，意味着 7 天不登录就变成"非活跃用户"，Fanout 推送时会跳过
- UserContext 用 ThreadLocal，Service 层不用每个方法都传 userId 参数

---

### 2. 内容发布

**做了什么**：发布纯文本、图文（最多 3 张图）、视频（含封面）。发布后内容先变成待审核状态，审核通过后才正式可见并推送给粉丝。

#### 请求链路：发布一条图文帖子（最完整的场景）

```
[第一步] 前端上传图片 → UploadController → MinioService
  │
  ├─ 生成 objectName: images/20260614/{uuid}.jpg
  └─ 上传到 MinIO bucket "buzzfeed"
  └─ 返回图片 URL 列表给前端

[第二步] 前端 POST /api/post/publish { contentType=1, title, description, imageUrls[] }
  │
  ▼
PostServiceImpl.postContent()  ← @Transactional
  │
  ├─ 1. 业务校验：图文至少 1 张图、最多 3 张
  │
  ├─ 2. MySQL: SELECT MAX(item_id)+1 生成新 ID（非自增，方便分库分表）
  │
  ├─ 3. MySQL: INSERT INTO item_info
  │     → status=0（待审核）, publish_time=null（审核通过后才写入）
  │
  ├─ 4. MySQL: batch INSERT INTO image_info（关联 item_id + 图片 URL + 排序）
  │
  └─ 5. Kafka: send("content-review", itemId)
        → 发布接口立即返回 { itemId }，不等审核结果
```

**代码位置**：

| 什么 | 在哪 |
|------|------|
| 发布接口 | controller/PostController.java |
| 发布逻辑 | service/impl/PostServiceImpl.java |
| 上传接口 | controller/UploadController.java |
| MinIO 操作 | service/MinioService.java |
| 视频转码 | service/VideoTranscodeService.java |
| 前端发布页 | views/Compose.vue |
| 首页快速发布 | components/post/PostComposer.vue |

**设计决策**：
- item_id 不用自增，用 MAX+1 手动生成，方便以后分库分表
- 发布和审核通过 Kafka 异步解耦，发布接口快速返回
- 视频上传时自动检测编码，H.265 转 H.264，因为 Chrome 不支持 HEVC
- 转码后的 MP4 加了 +faststart 标志，moov atom 前置，可以边下边播
- 视频自动截第 1 秒的画面作为封面

---

### 3. 内容审核 + Fanout 推送

**做了什么**：发布内容后不会立刻出现在粉丝的 Feed 里。后端有个 Kafka 消费者拿到发布消息后做审核（目前 MVP 阶段全部通过），审核通过后更新状态，再发一条消息触发 Fanout——把这条内容推送到每个粉丝的收件箱。

#### 请求链路：从发布到粉丝收到内容的完整管道

```
[阶段一] 审核：content-review topic
  │
  ▼
KafkaConsumerService.consumeContentReview(itemId)
  │
  ├─ 1. simulateReview() → MVP 阶段全部返回 1（通过）
  │
  ├─ 2. MySQL: UPDATE item_info SET status=1, publish_time=NOW() WHERE item_id=?
  │
  └─ 3. Kafka: send("feed-fanout-request", key=creatorId, body=FanoutMessage{itemId,creatorId,publishTime})
        → key=creatorId，保证同一创作者的消息落到同一分区、有序处理

[阶段二] Fanout 推送：feed-fanout-request topic（3 个线程并行消费）
  │
  ▼
FanoutConsumerService.onFanoutRequest(message)
  │
  ├─ 1. MySQL: followMapper.selectFollowerUserIds(creatorId)
  │     → 从 follower 表查出所有 type=1 的粉丝（取关的 type=2 已自动排除）
  │
  ├─ 2. MySQL: followMapper.countFollowers(creatorId) → 粉丝总数
  │
  ├─ 3. 判断是否大V：粉丝数 > bigVThreshold（默认 2）
  │     ├─ 非大V → targetFans = 全部粉丝
  │     └─ 大V   → 只推给活跃粉丝
  │           └─ 逐个检查 Redis: EXISTS active:user:{fanId}（登录过才有这个 key）
  │
  ├─ 4. MySQL: batch INSERT INTO inbox（每批 200 人）
  │     → (user_id, content_id, publish_time) 作为每个粉丝的收件箱
  │
  └─ 5. Redis: Pipeline 批量写入 inbox:{userId} ZSET（缓存加速，失败不阻塞）
        → score=publishTime 的时间戳, member=零补到20位的contentId
        → 每个 ZSET 最多保留 500 条（removeRange 裁剪）
```

**代码位置**：

| 什么 | 在哪 |
|------|------|
| 审核消费者 | service/KafkaConsumerService.java |
| Fanout 消费者 | service/FanoutConsumerService.java |
| Kafka 容器配置 | config/KafkaConfig.java |
| 消息体定义 | entity/FanoutMessage.java |
| Redis Feed 工具 | utils/RedisFeedHelper.java |

**设计决策**：
- 两个消费者用不同的 topic 和 group，解耦审核和推送
- Fanout 消费者配了 3 个线程并行消费，加快推送速度
- 失败重试 3 次，还不行就进死信队列（DLQ），不会阻塞正常消息
- Kafka Key = creatorId，保证同一个创作者的消息按顺序处理
- 大V只推给活跃粉丝 → 非活跃粉丝的收件箱会缺内容，需要 Feed 读取时用"推拉结合"补偿

---

### 4. Feed 信息流

**做了什么**：首页关注 Tab 展示你关注的人发的内容，按时间倒序，支持下拉刷新和上滑加载更多。

#### 请求链路：活跃用户首次加载 Feed（最常见场景，Redis 全程命中）

```
前端 GET /api/feed?type=0  ← 首次加载（无游标）
  │
  ▼
FeedServiceImpl.getFeed()
  │
  ├─ 1. MySQL: followMapper.selectFollowingUserIds(userId)
  │     → 查出当前用户关注了哪些人
  │
  ├─ 2. MySQL: userMapper.selectVbyId(follows) → 筛出其中的大V
  │
  ├─ 3. Redis: EXISTS active:user:{userId} → 判断当前用户是否活跃
  │     → 活跃用户的收件箱在 Fanout 阶段已被完整写入，不需要补偿
  │
  ├─ 4. 收件箱查询（Redis 优先，miss 回源 MySQL）：
  │     │
  │     ├─ Redis: ZREVRANGE inbox:{userId} 0 14 WITHSCORES
  │     │   → 拿到 15 条 contentId + score（时间戳）
  │     │
  │     ├─ Redis: Pipeline 批量 HGETALL content:{contentId}
  │     │   → 从 Hash 缓存拿内容详情（title、summary、creatorId 等）
  │     │   → 命中 → 直接用
  │     │   → 未命中 → MySQL: postMapper.selectById(contentId) 回源
  │     │
  │     └─ 如果整个 inbox ZSET 为空（Redis 全 miss）：
  │          → MySQL: SELECT * FROM inbox WHERE user_id=? ORDER BY publish_time DESC, content_id DESC LIMIT 15
  │
  ├─ 5. 活跃用户 + 有缓存 → 直接返回，不走发件箱归并
  │
  └─ 6. convertToVO()：
        ├─ 批量查 user 表（按 creatorId 去重，避免 N+1）
        ├─ 批量查 image_info / video_info（拼接 MinIO 完整 URL）
        └─ 组装 FeedItemVO 列表返回
```

#### 非活跃用户的推拉结合路径（补充说明）

如果第 3 步判断用户**非活跃**，且关注了大V，说明大V Fanout 时跳过了该用户，收件箱会漏内容：

```
  ├─ 5'. 非活跃 + 关注了大V → 需要补偿：
  │     │
  │     ├─ 对每个大V，查发件箱：
  │     │   Redis: ZREVRANGE outbox:{creatorId} 0 4 WITHSCORES
  │     │   → miss 则 MySQL: SELECT * FROM item_info WHERE creator_id=? AND status=1 ORDER BY ...
  │     │
  │     └─ PriorityQueue 多路归并（同 LeetCode 23: Merge k Sorted Lists）
  │         → 收件箱一路 + N 个大V发件箱各一路
  │         → 按 publishTime 降序取 Top N，自动去重（同一个 contentId 可能同时在收件箱和发件箱）
  │
  └─ 6. convertToVO() → 返回
```

**代码位置**：

| 什么 | 在哪 |
|------|------|
| Feed 接口 | controller/FeedController.java |
| Feed 逻辑（推拉结合+归并） | service/impl/FeedServiceImpl.java |
| Redis Feed 工具 | utils/RedisFeedHelper.java |
| 收件箱查询 SQL | mapper/InboxMapper.xml |
| 发件箱查询 SQL | mapper/OutboxMapper.xml |
| 前端 Feed 列表 | components/feed/FeedList.vue |
| Feed 状态管理 | stores/feed.js |

**设计决策**：
- 双游标 (publishTime, contentId) 解决同时间多条内容的分页问题
- Redis ZSET 的 member 用零补到 20 位的 contentId（如 `00000000000000000042`），保证字典序 = 数值序
- ZSET 游标分页：先取同 score 中 member < 游标的，再取更旧的 score，两步凑满 num 条
- content Hash 缓存只存首页展示字段（不含完整内容），Pipeline 批量读取
- 前端首次加载 15 条，后续每次加载 5 条

---

### 5. 关注/粉丝

**做了什么**：关注用户、取消关注、查看关注列表、查看粉丝列表（带是否互相关注的状态）。

#### 请求链路：关注一个用户（首次关注 + Redis 写入）

```
前端 POST /api/follow/{toUserId}
  │
  ▼
FollowServiceImpl.follow(fromUserId, toUserId)
  │
  ├─ 1. Redis: ZCARD following:{fromUserId}
  │     → 快速判断关注数是否已达上限 2000
  │
  ├─ 2. MySQL: SELECT * FROM following WHERE from_user_id=? AND to_user_id=?
  │     ├─ 不存在 → 首次关注
  │     ├─ type=2 → 重新关注（之前取关过）
  │     └─ type=1 → 已关注，直接 return
  │
  ├─ 3. MySQL: 双表写入（@Transactional 保证原子性）
  │     ├─ INSERT INTO following (from_user_id, to_user_id, type=1)
  │     └─ INSERT INTO follower  (from_user_id, to_user_id, type=1)
  │     或（重新关注）:
  │     ├─ UPDATE following SET type=1 WHERE from_user_id=? AND to_user_id=?
  │     └─ UPDATE follower  SET type=1 WHERE from_user_id=? AND to_user_id=?
  │
  └─ 4. Redis: 双 ZSET 写入（score=当前时间戳）
        ├─ ZADD following:{fromUserId} nowMs toUserId
        └─ ZADD followers:{toUserId}  nowMs fromUserId
```

#### 请求链路：查看粉丝列表 + 互相关注判断

```
前端 GET /api/user/{userId}/followers
  │
  ▼
FollowServiceImpl.getFollowerList(userId, size, myId)
  │
  ├─ 1. Redis: ZREVRANGE followers:{userId} 0 size
  │     ├─ 命中 → 直接拿到粉丝 userId 列表（按关注时间倒序）
  │     └─ 未命中 → MySQL: SELECT * FROM follower WHERE to_user_id=? AND type=1 ORDER BY id DESC
  │                   → 回写 Redis ZSET（缓存预热）
  │
  ├─ 2. 批量查 MySQL user 表 → 拿头像、昵称等
  │
  └─ 3. batchGetFollowStatus(myId, followerIds) → 互相关注判断：
        对每个 followerId：
        ├─ Redis: ZSCORE following:{myId} followerId  → 我是否关注了 ta
        │   ├─ 有分数 → 我关注了 ta
        │   │   └─ Redis: ZSCORE following:{followerId} myId  → ta 是否关注了我
        │   │       ├─ 有分数 → MUTUAL_FOLLOW（互相关注）
        │   │       └─ 无分数 → FOLLOWING（单向关注）
        │   └─ 无分数 → NOT_FOLLOWING（未关注）
        → 全部 O(1) 操作，不需要查 MySQL
```

**代码位置**：

| 什么 | 在哪 |
|------|------|
| 关注接口 | controller/FollowController.java |
| 关注逻辑 | service/impl/FollowServiceImpl.java |
| 关注关系 SQL | mapper/FollowMapper.xml |
| 前端关注按钮 | components/user/FollowButton.vue |
| 前端粉丝列表 | views/Followers.vue |
| 前端关注列表 | views/FollowingList.vue |

**设计决策**：
- 双表冗余写入（following + follower），但查谁关注了我/我关注了谁都走各自表，不需要反向索引
- 取关时不 DELETE，而是 UPDATE type=1 → type=2，这样 Fanout 查粉丝列表不会误推给已取关的人
- Redis ZSet 缓存关注列表，score=时间戳，查列表优先读 Redis，miss 了回 MySQL 并回写
- 互相关注判断纯 Redis O(1)，不需要回源 MySQL

---

### 6. 文件上传 + 视频转码

**做了什么**：图片和视频上传到 MinIO。视频上传时自动检测编码，H.265 转 H.264，同时截封面。

#### 请求链路：上传一个 H.265 编码的视频（最完整的场景）

```
前端 POST /api/upload (multipart/form-data, directory="videos")
  │
  ▼
UploadController → MinioService.upload(file, "videos")
  │
  ├─ 1. 确保 MinIO bucket "buzzfeed" 存在，不存在则创建
  │
  ├─ 2. 生成 objectName: videos/20260614/{uuid}.mp4
  │
  ├─ 3. 视频写入临时文件: tempInput
  │
  ├─ 4. FFmpeg: ffprobe 检测编码 → "hevc"（H.265）
  │
  ├─ 5. needsTranscode("hevc") → true
  │     └─ FFmpeg: 转码 H.265 → H.264
  │         参数: libx264 + AAC + preset=fast + crf=23 +faststart
  │         → 生成临时文件: transcoded
  │         → +faststart: moov atom 前置，支持边下边播
  │
  ├─ 6. MinIO: putObject(transcoded → objectName)  ← 上传转码后的视频
  │
  ├─ 7. FFmpeg: 截取第 1 秒画面作为封面
  │     → 临时文件: coverFile
  │     → MinIO: putObject(coverFile → videos/20260614/{uuid}_cover.jpg)
  │
  └─ 8. finally: 删除 tempInput、transcoded、coverFile 三个临时文件
        → 返回 { objectName, coverObjectName }
```

**代码位置**：

| 什么 | 在哪 |
|------|------|
| 上传接口 | controller/UploadController.java |
| MinIO 操作 | service/MinioService.java |
| 视频转码 | service/VideoTranscodeService.java |
| MinIO 配置 | config/MinioConfig.java |

**设计决策**：
- 文件路径按日期组织：`images/20260614/{uuid}.jpg`，便于按日期清理
- 用 ffprobe 检测编码，ffmpeg 转码，参数：libx264 + AAC + preset=fast + crf=23
- +faststart 让 moov atom 在文件头部，浏览器可以边下载边播放
- 转码和截帧都用临时文件，finally 块里删除，不会撑爆磁盘
- 最大支持 100MB 文件上传（application.yml 里配的）

---

### 7. 用户资料

**做了什么**：查看指定用户的公开资料（头像、昵称、bio、关注/粉丝数、帖子数），以及他们的帖子列表。

#### 请求链路：查看某用户的个人主页

```
前端 GET /api/user/{userId}
  │
  ▼
UserController.getUserProfile(userId)
  │
  ├─ MySQL: userMapper.selectById(userId)
  │   → 拿到 user 表所有公开字段
  │   → passwordHash 清空（不返回给前端）
  │
  └─ 返回 { userId, username, displayName, avatar, bio, followerNumber, followsNumber, postCount }

前端 GET /api/post/user/{userId}
  │
  ▼
PostServiceImpl.getUserPosts(userId, limit)
  │
  ├─ MySQL: SELECT * FROM item_info WHERE creator_id=? AND status=1 ORDER BY publish_time DESC LIMIT ?
  │
  └─ convertToVO()：
      ├─ 批量查 image_info / video_info
      └─ 组装返回
```

**代码位置**：

| 什么 | 在哪 |
|------|------|
| 用户接口 | controller/UserController.java |
| 用户 SQL | mapper/UserMapper.java（注解方式） |
| 前端个人主页 | views/Profile.vue |
| 前端资料弹窗 | components/user/UserProfileModal.vue |

---

## 前端架构

### 页面路由

| 路由 | 页面 | 干什么的 |
|------|------|---------|
| /login | Login.vue | 登录 |
| /register | Register.vue | 注册 |
| / | Home.vue | 首页 Feed 流（关注 Tab） |
| /compose | Compose.vue | 发布内容（文本/图文/视频） |
| /explore | Explore.vue | 发现页（占位） |
| /profile/:userId | Profile.vue | 用户个人主页 |
| /profile/:userId/followers | Followers.vue | 粉丝列表 |
| /profile/:userId/following | FollowingList.vue | 关注列表 |
| /post/:postId | PostDetail.vue | 内容详情 |

路由守卫在 router/index.js 里，没登录自动跳 /login，已登录访问 /login 自动跳首页。

### 状态管理（Pinia）

| Store | 文件 | 管什么 |
|-------|------|--------|
| auth | stores/auth.js | token、currentUser、登录/登出 |
| feed | stores/feed.js | 帖子列表、游标、加载状态 |

### API 拦截器

在 api/index.js 里：
- 请求拦截：自动给每个请求加 Authorization: Bearer <token>
- 响应拦截：code!=200 报错；401 自动登出跳登录页

### 核心组件

| 组件 | 干什么 |
|------|--------|
| FeedItem.vue | 单条内容卡片：文本/图文/视频展示，图片 Lightbox 预览，视频点击播放 |
| FeedList.vue | 无限滚动列表，用 IntersectionObserver 监听底部触发加载更多 |
| PostComposer.vue | 首页顶部的快速发布框（纯文本） |
| FollowButton.vue | 关注/取关按钮，点完自动刷新状态（可能是互相关注） |
| UserProfileModal.vue | 点用户头像弹出的资料卡片，有退出登录确认 |
| AppLayout.vue | 三栏布局：左边导航 + 中间内容 + 右边推荐 |

---

## 数据库表

| 表 | 干什么 | 主要字段 |
|----|--------|---------|
| user | 用户 | user_id, username, password_hash, display_name, avatar, bio, follower_number, follows_number, post_count |
| item_info | 内容主表（也当发件箱用） | item_id(非自增), creator_id, item_type(0文本/1图文/2视频), title, summary, status(0待审/1正常/2删除), publish_time |
| image_info | 图文的图片 | id, item_id, image_uri, sort_order |
| video_info | 视频信息 | id, item_id, video_url, cover_url, duration |
| following | 关注关系（我关注了谁） | id, from_user_id, to_user_id, type(1关注/2取消) |
| follower | 关注关系（谁关注了我） | id, from_user_id, to_user_id, type(1关注/2取消) |
| inbox | 收件箱（Feed推送结果） | user_id, content_id, publish_time |

---

## 中间件

| 中间件 | 端口 | 干什么 |
|--------|------|--------|
| MySQL | 3306 | 存所有数据，库名 buzzfeed，密码 1234 |
| Redis | 6379 | 关注列表缓存、用户活跃标记、密码 123456 |
| Kafka | 9092 | 两个 topic：content-review（审核）和 feed-fanout-request（推送） |
| MinIO | 9000 | 存图片和视频，bucket 名 buzzfeed，账号 minioadmin |
| FFmpeg | 本地安装 | 视频转码和封面截帧 |

---

## 项目结构

    BuzzFeed/
    +-- src/main/java/org/sdb/buzzfeed/
    |   +-- BuzzFeedApplication.java          启动类
    |   +-- config/                            配置
    |   |   +-- SecurityConfig.java            Security + CORS
    |   |   +-- WebConfig.java                 拦截器注册
    |   |   +-- KafkaConfig.java               Kafka 消费者工厂
    |   |   +-- MinioConfig.java               MinIO 客户端
    |   |   +-- GlobalExceptionHandler.java    全局异常处理
    |   +-- controller/                        接口层
    |   +-- service/                           业务层
    |   +-- entity/                            实体、DTO、VO
    |   +-- mapper/                            MyBatis 接口
    |   +-- utils/                             JWT、ThreadLocal、RedisFeedHelper
    +-- src/main/resources/
    |   +-- application.yml                    全局配置
    |   +-- mapper/*.xml                       SQL 映射
    +-- Frontend/src/
    |   +-- api/                               API 调用
    |   +-- components/                        组件
    |   +-- views/                             页面
    |   +-- stores/                            Pinia 状态
    |   +-- composables/                       组合式函数
    |   +-- router/                            路由
    +-- pom.xml

---

## 还没做的

- 推荐算法（首页推荐 Tab 目前是占位）
- 点赞（前端组件有，后端没接）
- 评论（前端本地模拟，后端没做）
- 通知系统
- 私信系统
- 头像上传
- 内容搜索
- 审核逻辑（目前 MVP 全部通过）