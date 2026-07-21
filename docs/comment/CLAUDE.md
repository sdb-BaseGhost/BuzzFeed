# 评论模块 (Comment)

## 概述

支持对帖子发表评论及二级回复，评论数 Redis 计数，Kafka 异步通知被评论者。

## 涉及文件 (待实现)

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/CommentController.java` | 评论相关端点 |
| Service | `service/CommentService.java` + `impl/CommentServiceImpl.java` | 评论业务逻辑 |
| Mapper | `mapper/CommentMapper.java` + `.xml` | 评论 CRUD |
| 实体 | `entity/Comment.java` | 评论实体 (待建) |

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

## 设计决策

- 二级回复通过 parent_id 关联父评论，reply_user_id 记录被回复者
- 评论数用 Redis STRING 计数，INCR on create / DECR on delete
- 评论列表用游标分页 (lastCommentId)，按时间倒序
- Kafka 通知被评论者/被回复者 (Topic: comment-notify)
