# 点赞模块 (Like)

## 概述

对帖子点赞/取消点赞，Redis 计数，支持查询"谁赞了"和"我是否已赞"。

## 涉及文件 (待开发)

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/LikeController.java` | 点赞相关端点 |
| Service | `service/LikeService.java` + `impl/LikeServiceImpl.java` | 点赞业务逻辑 |
| Mapper | `mapper/LikeMapper.java` + `.xml` | 点赞记录 CRUD |
| 实体 | `entity/Like.java` | 点赞实体 (待建) |

## API 设计

```
POST   /like/toggle               点赞/取消点赞 (幂等)
  Body: { itemId }
  返回: { liked: true/false, likeCount: N }

GET    /like/status               批量查询点赞状态
  Params: itemIds=1,2,3
  返回: { 1: true, 2: false, 3: true }

GET    /like/list                 点赞列表 (谁赞了这条)
  Params: itemId, lastLikeId(游标), size(默认20)
```

## 数据库表 (待建)

```sql
CREATE TABLE item_like (
  like_id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id       BIGINT NOT NULL,            -- 内容ID
  user_id       BIGINT NOT NULL,            -- 点赞者ID
  create_time   DATETIME(3) NOT NULL,
  UNIQUE KEY uk_item_user (item_id, user_id),  -- 幂等：一人只能赞一次
  INDEX idx_user_id (user_id, create_time)
);
```

## Redis 设计

```
like:set:{itemId}    SET      点赞用户集合 (SADD/SREM/SISMEMBER)
like:count:{itemId}  STRING   点赞计数 (INCR/DECR, 定期从DB校准)
```

## 数据一致性

```
写入流程:
  1. INSERT item_like (唯一索引防重复)
  2. SADD like:set:{itemId} userId
  3. INCR like:count:{itemId}
  取消时: DELETE + SREM + DECR

Canal 同步 (可选):
  监听 item_like 表 INSERT/DELETE → 同步 Redis SET 和计数
```

## 待开发

- [ ] Like 实体 + 表 + Mapper
- [ ] 点赞/取消点赞 (幂等, 唯一索引)
- [ ] Redis SET 记录点赞用户 (SISMEMBER 判断状态)
- [ ] Redis 点赞计数
- [ ] 批量查询点赞状态 (pipeline)
- [ ] 点赞列表 (游标分页)
- [ ] item_info 表 like_count 字段同步
