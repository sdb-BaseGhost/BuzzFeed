# 发帖模块 (Post)

## 概述

支持三种内容类型发布：纯文本(0)、图文(1，最多3张)、视频(2)。发布后通过 Kafka 异步审核。

## 涉及文件

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/PostController.java` | `POST /post` multipart 接口 |
| Service | `service/PostService.java` + `impl/PostServiceImpl.java` | 发帖主逻辑：校验 → 写DB → 上传MinIO → 发Kafka |
| Service | `service/MinioService.java` | MinIO 文件上传 (按日期分目录，UUID 命名) |
| Service | `service/KafkaConsumerService.java` | 监听 `content-review` topic，模拟审核 |
| Mapper | `mapper/postMapper.java` + `.xml` | insertContent / updateState / getNextItemId |
| Mapper | `mapper/ContentImageMapper.java` + `.xml` | 批量插入图片记录 |
| Mapper | `mapper/ContentVideoMapper.java` + `.xml` | 插入视频记录 |
| Config | `config/MinioConfig.java` | MinIO 客户端 Bean |
| 实体 | `entity/Content.java` | itemId / creatorId / itemType / title / summary / visibility / status / publishTime |
| 实体 | `entity/ContentImage.java` | id / itemId / imageUri / sortOrder |
| 实体 | `entity/ContentVideo.java` | id / itemId / creatorId / bucketName / objectName / videoUrl / duration / fileSize |

## 发帖链路

```
POST /post (contentType, title, description, visibility, images[], video)
    ↓
1. 参数校验 (contentType/title 非空, 图文至少1张, 视频必传)
    ↓
2. 生成 itemId (postMapper.getNextItemId, 非自增主键)
    ↓
3. INSERT item_info (status=0 待审核)
    ↓
4. 上传 MinIO → INSERT image_info / video_info
    ↓
5. KafkaTemplate.send("content-review", itemId)
    ↓
6. KafkaConsumerService.consumeContentReview()
   ├─ simulateReview() → 全部通过 (MVP)
   ├─ postMapper.updateState(itemId, 1) → 审核通过
   └─ TODO: 审核通过后触发 Feed Fan-out 推送
```

## API 设计

```
POST /post
Content-Type: multipart/form-data

参数:
  contentType  : 0=纯文本 1=图文 2=视频 (必填)
  title        : 标题 (必填)
  description  : 描述/摘要 (选填)
  visibility   : 0私密 1好友 2粉丝 3公开 (默认1)
  images       : 图片文件数组 (图文类型必填, 最多3张)
  video        : 视频文件 (视频类型必填)

返回: itemId (Long)
```

## 数据库表

- `item_info` — 内容主表，PK: item_id (非自增), IDX: creator_id
- `image_info` — 图片表，FK: item_id + version + status
- `video_info` — 视频表，FK: creator_id + status
- `item_text` — 文本版本表，UK: item_id + version

详见 `docs/table.md`

## 已完成

- [x] 图文发布 (MinIO 上传 + DB 落库)
- [x] 视频发布 (MinIO 上传 + DB 落库)
- [x] Kafka 异步审核 (content-review topic, MVP 全部通过)
- [x] item_id 非自增主键生成

## 待开发

- [ ] 纯文本发布 (item_text 表写入)
- [ ] 审核通过后触发 Kafka feed-fanout-request 推送
- [ ] 内容删除/下架接口
- [ ] 内容编辑 (版本管理: online_version / latest_version)
- [ ] 帖子详情接口 (含图片/视频 URL 拼接)
