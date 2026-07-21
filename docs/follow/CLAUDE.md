# 关注/粉丝模块 (Follow)

## 概述

关注/取关用户，查看关注列表和粉丝列表，判断互相关注状态。DB + Redis ZSET 双写，互相关注判断纯 Redis O(1)。

## 涉及文件

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/FollowController.java` | POST/DELETE /api/follow, GET following/followers/status |
| Controller | `controller/UserController.java` | GET /api/user/{userId}, GET /api/user/{userId}/following,followers |
| Service | `service/FollowService.java` + `impl/FollowServiceImpl.java` | ⭐ 关注/取关/关系查询 (DB+Redis 双写) |
| Mapper | `mapper/FollowMapper.java` + `.xml` | following/follower 双表 CRUD |
| 实体 | `entity/FollowRelation.java` | 关注关系 (following/follower 表共用结构) |

## API 设计

```
POST   /api/follow                 关注用户
  Body: { toUserId }

DELETE /api/follow/{toUserId}      取消关注

GET    /api/follow/following/{userId}   关注列表
  Params: size(默认20)

GET    /api/follow/followers/{userId}   粉丝列表 (附加互相关注状态)
  Params: size(默认20)

GET    /api/follow/status/{targetUserId}  关注关系状态
  返回: { status: "FOLLOWING" | "NOT_FOLLOWING" | "MUTUAL_FOLLOW" }
```

## 数据库表

```
following: id / from_user_id / to_user_id / type(1关注/2取消) / create_time / update_time
follower:  id / from_user_id / to_user_id / type(1关注/2取消) / create_time / update_time
```

两张表冗余存储，查"我关注了谁"走 following，查"谁关注了我"走 follower。

## 设计决策

- 双表冗余写入 (following + follower)，读取各自表不需要反向索引
- 取关不 DELETE，UPDATE type=1→type=2，Fanout 查粉丝列表不会误推给已取关的人
- Redis ZSET 缓存关注/粉丝列表，score=时间戳，miss 了回 MySQL 并回写
- 互相关注判断纯 Redis: ZSCORE following:{myId} targetId + ZSCORE following:{targetId} myId → O(1)
- 关注上限 2000，用 Redis ZCARD 快速判断
- 关注关系变更影响 Feed 模块: 新增粉丝后 Fanout 会推内容给新粉丝