# Feed 流模块

## 概述

推拉结合 Feed 流，参考《亿级流量系统架构设计与实战》。活跃用户走推模式(收件箱)，大V走拉模式(发件箱)，读取时多路归并。

## 涉及文件

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/FeedController.java` | `POST /feed/getFeed` 接口 |
| Service | `service/FeedService.java` + `impl/FeedServiceImpl.java` | ⭐ 核心：推拉结合 + 多路归并 |
| Mapper | `mapper/inboxMapper.java` + `.xml` | 收件箱 CRUD (推模式写入/读取) |
| Mapper | `mapper/outboxMapper.java` + `.xml` | 发件箱查询 (大V 拉模式) |
| Mapper | `mapper/userMapper.java` + `.xml` | 活跃判断 / 关注列表 / 大V筛选 |
| 实体 | `entity/Feed.java` | userId / type / lastTime / contentId / num |

## 核心架构

```
用户请求 Feed
    ↓
1. 查询用户是否活跃 (user.is_active)
2. 查询用户关注列表
3. 筛选出大V (follower_number > 阈值)
    ↓
活跃用户 (is_active = 0):
  → 直接查 inbox 收件箱 (推模式，已经由 Fan-out 写入)

非活跃用户 (is_active = 1):
  → inbox 收件箱 (普通关注者推送的结果)
  + 大V 发件箱 (实时从 item_info 拉取)
  → PriorityQueue 多路归并
    ↓
返回 Top 5 条 Content
```

## 推拉结合策略

| 条件 | 策略 | 存储 |
|------|------|------|
| 普通用户发帖 + 活跃粉丝 | **推**：Fan-out 写入粉丝 inbox | inbox 表 + Redis ZSET |
| 大V发帖 | **拉**：不 Fan-out，粉丝实时读 outbox | item_info 表 + Redis ZSET |
| 读 Feed + 活跃用户 | 读 inbox 即可 | Redis → MySQL 兜底 |
| 读 Feed + 非活跃用户 | inbox + 大V outbox 多路归并 | Redis → MySQL 兜底 |

## 数据模型

- **收件箱 inbox**: `{user_id, content_id, publish_time}` — 每条是"谁应该看到什么"
- **发件箱 item_info**: `{item_id, creator_id, publish_time}` — 内容原文
- **大V阈值**: 当前 `follower_number > 2` (测试值，生产 ≥ 10000)

## Redis 设计

```
feed:inbox:{userId}     ZSET   score = ts*1_000_000 + contentId   TTL: 7天, 上限500条
feed:outbox:{userId}    ZSET   score = ts*1_000_000 + contentId   TTL: 30天, 上限1000条
follow:set:{userId}     SET    关注列表
fans:set:{userId}       SET    粉丝列表 (仅大V缓存)
```

## API 设计

```
POST /feed/getFeed
Content-Type: application/json

Body:
  userId     : Long       当前用户ID
  type       : Integer    0=下拉刷新(更新的内容) 1=上滑加载更多(更旧的内容)
  lastTime   : DateTime   最后一条Feed的发布时间 (游标)
  contentId  : Long       最后一条Feed的内容ID (游标去重)
  num        : int        每页条数 (默认5)

游标分页原理:
  type=0: WHERE publish_time > lastTime ORDER BY publish_time DESC LIMIT 5
  type=1: WHERE publish_time < lastTime OR (publish_time=lastTime AND content_id<contentId)
          ORDER BY publish_time DESC LIMIT 5
```

## 多路归并算法 (FeedServiceImpl.java)

```
1. inbox 数据作为一路
2. 每个大V的 outbox 各作为一路
3. 全部放入 PriorityQueue (按 publish_time 降序)
4. 每次 poll 堆顶 → 加入 result → 推入该路的下一条
5. 直到 result 满 5 条
```

## Kafka 设计 (Fan-out)

| Topic | Key | 生产者 | 消费者 |
|-------|-----|--------|--------|
| `content-review` | contentId | PostServiceImpl | KafkaConsumerService |
| `feed-fanout-request` | creatorId | KafkaConsumerService (审核通过后) | FanoutConsumerService (待开发) |

## 已完成

- [x] 推拉结合原型 (active 判断 + 大V筛选 + 多路归并)
- [x] 游标分页 (publish_time + content_id)
- [x] 下拉/上滑两种操作类型

## 待开发 (Phase 1)

- [ ] Redis 收件箱缓存 (ZSET，替代直接查 MySQL inbox)
- [ ] Redis 发件箱缓存 (ZSET，替代 outboxMapper IN 查询)
- [ ] Fan-out Consumer: 审核通过后推送给活跃粉丝
- [ ] Redis 关注关系缓存 (SET)
- [ ] buildScore() 方法实际接入 ZSET 操作
- [ ] 收件箱容量控制 (ZREMRANGEBYRANK 保留最新500条)
- [ ] Canal 监听 inbox 表变更 → 同步 Redis
