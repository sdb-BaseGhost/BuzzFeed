# Feed 流模块

## 概述

推拉结合 Feed 流，参考《亿级流量系统架构设计与实战》。活跃用户走推模式(收件箱)，大V走拉模式(发件箱)，读取时多路归并。三阶段异步管道：审核 → 缓存同步 → 推送。

## 涉及文件

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/FeedController.java` | `POST /feed/getFeed` 接口 |
| Service | `service/FeedService.java` + `impl/FeedServiceImpl.java` | ⭐ 核心：推拉结合 + 多路归并 |
| Service | `service/FanoutDispatcher.java` | ⭐ Kafka 消费者: 拉取粉丝 + 大V判断 + 动态分片 → 发送子任务 |
| Service | `service/FanoutExecutor.java` | ⭐ Kafka 消费者: 并行消费子任务 → 批量写 inbox (MySQL + Redis) |
| Service | `service/ContentCacheConsumer.java` | ⭐ Canal binlog 消费者: 维护 content Hash + outbox |
| Service | `service/ContentCacheService.java` | 内容 Redis Hash 缓存业务层读写 |
| 工具 | `utils/RedisFeedHelper.java` | ⭐ inbox/outbox ZSET + content Hash + 游标分页 |
| Config | `config/KafkaConfig.java` | Dispatcher(concurrency=1) + Executor(concurrency=可配置) 容器工厂 |
| Mapper | `mapper/InboxMapper.java` + `.xml` | 收件箱 CRUD (INSERT IGNORE 幂等) |
| Mapper | `mapper/OutboxMapper.java` + `.xml` | 发件箱查询 (大V 拉模式) |
| 实体 | `entity/Feed.java` | userId / type / lastTime / contentId / num |
| 实体 | `entity/FanoutMessage.java` | Kafka 消息体: contentId / creatorId / publishTime |
| 实体 | `entity/FanoutSubTask.java` | 分片子任务: contentId / taskId / totalShards / publishTime / fanIds |

## 核心架构

```
用户请求 Feed
    ↓
1. Redis: EXISTS active:user:{userId} → 判断是否活跃
2. MySQL: followMapper.selectFollowingUserIds(userId) → 关注列表
3. MySQL: userMapper.selectVbyId(follows) → 筛出大V
    ↓
活跃用户 (Redis 有 active key):
  → 直接读 inbox ZSET + content Hash 缓存

非活跃用户 (无 active key):
  → inbox ZSET (普通关注者推送的结果)
  + 每个大V的 outbox ZSET
  → PriorityQueue 多路归并
    ↓
返回 FeedItemVO 列表
```

## 推拉结合策略

| 条件 | 策略 | 存储 |
|------|------|------|
| 普通用户发帖 + 粉丝 | **推**：Fan-out 写入粉丝 inbox | MySQL inbox + Redis ZSET |
| 大V发帖 (粉丝 > 阈值) | **推给活跃粉丝**，非活跃粉丝不推 | 活跃判断走 Redis `active:user:{id}` |
| 读 Feed + 活跃用户 | 读 inbox 即可 | Redis ZSET → MySQL 兜底 |
| 读 Feed + 非活跃用户 | inbox + 大V outbox 多路归并 | Redis ZSET → MySQL 兜底 |

## 三阶段异步管道

```
PostServiceImpl → Kafka("content-review")
    ↓
KafkaConsumerService (审核, MVP全部通过)
  ├─ UPDATE item_info SET status=1, publish_time=NOW()
  ├─ Kafka("content-change")         → 通知 Canal 缓存同步
  └─ Kafka("feed-fanout-request")    → 触发 Fan-out 推送
    ↓
ContentCacheConsumer (监听 content-change)
  ├─ INSERT/UPDATE(status=1): HSET content:{id} + ZADD outbox:{creatorId}
  └─ UPDATE(status≠1)/DELETE: DEL content:{id} + ZREM outbox:{creatorId}
    ↓
FanoutDispatcher (监听 feed-fanout-request, 单线程, concurrency=1)
  ├─ followMapper.selectFollowerUserIds(creatorId) → 粉丝列表
  ├─ 大V → 只推活跃粉丝 (检查 Redis active:user:{fanId})
  ├─ 非大V → 推全部粉丝
  ├─ 排序 + 动态分片 (每片 shardSize=5000 人)
  └─ 逐片发送 Kafka("fanout-subtask")
    ↓
FanoutExecutor (监听 fanout-subtask, 多线程并行, concurrency=可配置)
  ├─ batch INSERT IGNORE inbox (幂等, 唯一键保证)
  └─ Pipeline ZADD inbox:{userId} ZSET (失败不阻塞)
```

## Redis 设计

```
inbox:{userId}         ZSET   score=publishTime时间戳, member=零补20位contentId   上限500条
outbox:{creatorId}     ZSET   同上
content:{contentId}    Hash   itemId/creatorId/itemType/title/summary/publishTime/status
active:user:{userId}   String TTL=7天, 登录时写入, 判断活跃用户
following:{userId}     ZSET   score=关注时间戳, member=被关注者userId
followers:{userId}     ZSET   score=关注时间戳, member=关注者userId
```

## API 设计

```
POST /feed/getFeed
Content-Type: application/json

Body:
  type       : Integer    0=下拉刷新(更新的内容) 1=上滑加载更多(更旧的内容)
  lastTime   : DateTime   最后一条Feed的发布时间 (游标)
  contentId  : Long       最后一条Feed的内容ID (游标去重)
  num        : int        每页条数 (默认15)

游标分页原理:
  type=0: WHERE publish_time > lastTime ORDER BY publish_time DESC LIMIT n
  type=1: WHERE publish_time < lastTime OR (publish_time=lastTime AND content_id<contentId)
          ORDER BY publish_time DESC LIMIT n
```

## ZSET 游标分页算法 (RedisFeedHelper)

```
contentId 零补到20位: 42 → "00000000000000000042"
  → 保证 ZSET member 字典序 = 数值序

首次加载: ZREVRANGE key 0 num-1 WITHSCORES

加载更多 (两步查询):
  Step 1: score=cursorScore 且 member < cursorMember → 同批次更早内容
  Step 2: score < cursorScore → 更早时间段
  两步凑满 num 条，等价于 MySQL 联合索引 (publish_time DESC, content_id DESC)
```

## 设计决策

- 三阶段管道完全解耦: 审核/缓存同步/推送各自独立 topic
- Fan-out 拆分为 Dispatcher + Executor: Dispatcher 单线程保证分片确定性，Executor 多线程并行写入
- Dispatcher → Executor 通过 Kafka("fanout-subtask") 解耦，子任务 Key = contentId
- 大V只推活跃粉丝 → 非活跃粉丝收件箱缺内容 → Feed 读取时推拉结合补偿
- inbox INSERT IGNORE 幂等: 分片重试或重复消费不会产生重复数据
- Redis 写入失败 catch 住，MySQL 是主存储，Redis 是加速缓存
- Canal 缓存同步: 本地用 Kafka 消息模拟 binlog，生产切 Canal 零成本
- content Hash 只存首页展示字段 (不含完整内容)，Pipeline 批量读避免 N+1
