# 用户关系模块 - 执行文档

## 一、数据库

### 1. 废弃旧表

`table.md` 中的 `user_follow` 表已删除，该表在新设计中由 `following` 和 `follower` 两张表替代。

### 2. DDL

```sql
-- Following 表：以关注方向为索引 (我关注了谁)
CREATE TABLE `following` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT NOT NULL COMMENT '关注者ID',
  `to_user_id`   BIGINT NOT NULL COMMENT '被关注者ID',
  `type`         TINYINT NOT NULL DEFAULT '1' COMMENT '关系状态：1-正在关注 2-取消关注',
  `create_time`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_from_to` (`from_user_id`, `to_user_id`),
  KEY `idx_following_list` (`from_user_id`, `type`, `update_time`, `to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关注关系表';

-- Follower 表：以粉丝方向为索引 (谁关注了我)
CREATE TABLE `follower` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT NOT NULL COMMENT '关注者ID',
  `to_user_id`   BIGINT NOT NULL COMMENT '被关注者ID',
  `type`         TINYINT NOT NULL DEFAULT '1' COMMENT '关系状态：1-正在关注 2-取消关注',
  `create_time`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_to_from` (`to_user_id`, `from_user_id`),
  KEY `idx_follower_list` (`to_user_id`, `type`, `update_time`, `from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='粉丝关系表';
```

### 3. 索引说明

| 表 | 索引 | 用途 |
|----|------|------|
| following | `uk_from_to` | 关注/取关时定位唯一记录，保证幂等 |
| following | `idx_following_list` | 查询"我的关注列表"：from_user_id + type 过滤，update_time DESC 排序，to_user_id 覆盖索引免回表 |
| follower | `uk_to_from` | 粉丝方向定位唯一记录 |
| follower | `idx_follower_list` | 查询"我的粉丝列表"：to_user_id + type 过滤，update_time DESC 排序，from_user_id 覆盖索引免回表 |

---

## 二、Redis Key 设计

| Key | 类型 | Member/Field | Score/Value | 说明 |
|-----|------|-------------|-------------|------|
| `following:{userId}` | ZSet | targetUserId | followTime | 我的关注列表，全量缓存（上限2000） |
| `followers:{userId}` | ZSet | followerUserId | followTime | 我的粉丝列表，缓存最近10000条 |
| `follow_relation:{targetUserId}` | Hash | userId | 1/0 | 粉丝关系缓存，Phase2实现 |

---

## 三、后端新增文件

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/FollowController.java` | 关注/取关/列表/关系查询端点 |
| Entity | `entity/FollowRelation.java` | 关注关系实体，映射 following 表 |
| Mapper | `mapper/FollowMapper.java` + `mapper/FollowMapper.xml` | following + follower 两张表的 CRUD |
| Service | `service/FollowService.java` | 关注模块接口 |
| Service Impl | `service/impl/FollowServiceImpl.java` | 核心业务：同步写双表 + 更新Redis |

## 四、前端新增/修改文件

| 文件 | 变更 |
|------|------|
| `api/follow.js` | 新增：关注/取关/关注列表/粉丝列表/关系查询 API |
| `stores/follow.js` | 新增：关注关系状态管理 |
| `components/user/FollowButton.vue` | 修改：接入真实接口，支持 FOLLOWING / NOT_FOLLOWING / MUTUAL_FOLLOW 三种状态 |
| `views/Followers.vue` | 修改：接入真实粉丝列表接口 |
| `views/FollowingList.vue` | 修改：真实关注列表接口 |
| `views/Profile.vue` | 修改：展示关注数/粉丝数，显示关注状态按钮 |

---

## 五、接口设计

### 5.1 关注用户

```
POST /api/follow
Body: { "toUserId": 1002 }
返回: Result<Void>

前置校验：
  - 不能关注自己
  - 关注数 < 2000（查询 Redis ZCARD following:{myId}）
  - 幂等：重复关注返回成功

写入流程：
  1. SELECT following 表 WHERE from_user_id=myId AND to_user_id=targetId
  2. 不存在 → INSERT following + INSERT follower (type=1)
     已存在且 type=2 → UPDATE following SET type=1 + UPDATE follower SET type=1
     已存在且 type=1 → 直接返回成功（幂等）
  3. ZADD following:{myId} timestamp targetId
  4. ZADD followers:{targetId} timestamp myId
  5. 异步更新关注数/粉丝数缓存（Phase2）
```

### 5.2 取消关注

```
DELETE /api/follow/{toUserId}
返回: Result<Void>

写入流程：
  1. SELECT following 表 WHERE from_user_id=myId AND to_user_id=targetId AND type=1
  2. 不存在或 type=2 → 返回成功（幂等）
  3. UPDATE following SET type=2, update_time=NOW()
  4. UPDATE follower  SET type=2, update_time=NOW()
  5. ZREM following:{myId} targetId
  6. ZREM followers:{targetId} myId
```

### 5.3 查询关注列表

```
GET /api/follow/following/{userId}?cursor={timestamp}&size={20}
返回: Result<{ list: [{ userId, nickname, avatar, followStatus }], cursor, hasMore }>

读取流程：
  1. ZREVRANGEBYSCORE following:{userId} (cursor INF LIMIT size+1
  2. 命中 → 取 size+1 条判断 hasMore，批量查用户信息返回
  3. 未命中 → 回源 DB：SELECT * FROM following WHERE from_user_id=userId AND type=1 ORDER BY update_time DESC LIMIT size+1
     回源后写入 Redis ZSet（异步）
```

### 5.4 查询粉丝列表

```
GET /api/follow/followers/{userId}?cursor={timestamp}&size={20}
返回: Result<{ list: [{ userId, nickname, avatar, followStatus }], cursor, hasMore }>

读取流程：
  1. ZREVRANGEBYSCORE followers:{userId} (cursor INF LIMIT size+1
  2. 命中 → 批量查用户信息
  3. 未命中 → 回源 DB：SELECT * FROM follower WHERE to_user_id=userId AND type=1 ORDER BY update_time DESC LIMIT size+1
     回源后写入 Redis ZSet
```

### 5.5 查询与粉丝列表用户的关系（列表页附加）

```
在粉丝列表返回时，需要对每个粉丝用户附加"我是否关注了他"的状态。

批量查询流程（在粉丝列表接口内部调用）：
  对粉丝列表中的 userIds，批量查询 following:{myId}
  ZSCORE following:{myId} userId → 存在则为已关注，否则为未关注
  组合判断：
    - 我关注了他 + 他关注了我 → MUTUAL_FOLLOW
    - 我关注了他 + 他没关注我 → FOLLOWING
    - 我没关注他              → NOT_FOLLOWING
```

### 5.6 查询关系状态（单独接口）

```
GET /api/follow/status/{targetUserId}
返回: Result<{ status: "FOLLOWING" | "NOT_FOLLOWING" | "MUTUAL_FOLLOW" }>

流程：
  Pipeline 执行两条命令：
    ZSCORE following:{myId} targetId
    ZSCORE following:{targetId} myId
  组合判断三种状态
```

---

## 六、实现步骤（Phase1 执行顺序）

### Step 1：数据库
- [x] 从 `table.md` 删除旧 `user_follow` 表定义
- [x] 执行 following / follower 建表 DDL

### Step 2：后端基础
- [ ] 新建 `entity/FollowRelation.java`
- [ ] 新建 `mapper/FollowMapper.java` + `FollowMapper.xml`
  - `insertFollowing` / `insertFollower`
  - `updateFollowingType` / `updateFollowerType`
  - `selectFollowing` (by fromUserId + toUserId)
  - `selectFollowingList` (分页，idx_following_list)
  - `selectFollowerList` (分页，idx_follower_list)

### Step 3：后端核心
- [ ] 新建 `service/FollowService.java` (接口)
- [ ] 新建 `service/impl/FollowServiceImpl.java`
  - `follow(fromUserId, toUserId)` — 同步写双表 + 更新 Redis
  - `unfollow(fromUserId, toUserId)` — 同步更新双表 type=2 + 更新 Redis
  - `getFollowingList(userId, cursor, size)` — Redis 优先，miss 回源 DB
  - `getFollowerList(userId, cursor, size)` — Redis 优先，miss 回源 DB
  - `getFollowStatus(myId, targetId)` — 组合判断三种状态
  - `batchGetFollowStatus(myId, targetIds)` — 批量判断，用于粉丝列表附加状态

### Step 4：后端接口
- [ ] 新建 `controller/FollowController.java`
  - `POST /api/follow`
  - `DELETE /api/follow/{toUserId}`
  - `GET /api/follow/following/{userId}`
  - `GET /api/follow/followers/{userId}`
  - `GET /api/follow/status/{targetUserId}`

### Step 5：前端
- [ ] 新建 `api/follow.js` — 封装所有关注接口
- [ ] 新建 `stores/follow.js` — 关注状态管理（可选，也可在组件内直接调用）
- [ ] 修改 `components/user/FollowButton.vue` — 接入真实接口，三种状态显示
- [ ] 修改 `views/Profile.vue` — 展示关注/粉丝数，关注按钮
- [ ] 修改 `views/Followers.vue` — 接入真实粉丝列表
- [ ] 修改 `views/FollowingList.vue` — 接入真实关注列表

### Step 6：验证
- [ ] Postman/前端测试：关注 → 粉丝列表出现 → 取关 → 粉丝列表消失
- [ ] Redis 数据验证：ZSet 数据与 DB 一致
- [ ] 幂等验证：重复关注/取关不报错
- [ ] 状态验证：三种 followStatus 正确返回
