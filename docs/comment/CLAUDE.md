# 评论模块 (Comment)

## 概述

支持对帖子发表评论及二级回复，评论数 Redis 计数，Kafka 异步通知被评论者。

## 涉及文件 (待开发)

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/CommentController.java` | 评论相关端点 |
| Service | `service/CommentService.java` + `impl/CommentServiceImpl.java` | 评论业务逻辑 |
| Mapper | `mapper/CommentMapper.java` + `.xml` | 评论 CRUD |
| 实体 | `entity/Comment.java` | 评论实体 (待建) |
| Service | `KafkaConsumerService.java` | 扩展：监听评论通知消息 |

## API 设计

```
POST   /comment                  发表评论
  Body: { itemId, content, parentCommentId(选填,空=一级评论) }
  返回: commentId

GET    /comment/list             评论列表 (游标分页)
  Params: itemId, lastCommentId(游标), size(默认20)

DELETE /comment/{commentId}       删除自己的评论
```

## 数据库表 (待建)

```sql
CREATE TABLE comment (
  comment_id       BIGINT PRIMARY KEY,        -- 评论ID
  item_id          BIGINT NOT NULL,           -- 所属内容ID
  user_id          BIGINT NOT NULL,           -- 评论者ID
  parent_id        BIGINT DEFAULT 0,          -- 父评论ID (0=一级评论)
  reply_user_id    BIGINT DEFAULT 0,          -- 被回复者ID (二级评论时)
  content          VARCHAR(1000) NOT NULL,    -- 评论内容
  like_count       INT DEFAULT 0,             -- 点赞数
  create_time      DATETIME(3) NOT NULL,
  INDEX idx_item_id (item_id, create_time),
  INDEX idx_user_id (user_id)
);
```

## Redis 设计

```
comment:count:{itemId}   STRING   评论计数 (INCR/DECR)
```

## Kafka 通知 (可选)

```
Topic: comment-notify
Key:   itemId
Value: { commentId, itemId, userId, replyUserId }
→ 消费者: 推送通知给被评论者/被回复者
```

## 待开发

- [ ] Comment 实体 + 表 + Mapper
- [ ] 发表评论 (一级 + 二级回复)
- [ ] 评论列表 (游标分页，按时间倒序)
- [ ] 删除评论 (软删除/仅自己的)
- [ ] Redis 评论计数 (INCR on create, DECR on delete)
- [ ] Kafka 评论通知
