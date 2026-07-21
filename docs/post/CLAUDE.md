# 发帖模块 (Post)

## 概述

支持三种内容类型发布：纯文本(0)、图文(1，最多3张)、视频(2)。发布后通过 Kafka 异步审核，审核通过后触发 Fan-out 推送到粉丝收件箱。

## 涉及文件

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `controller/PostController.java` | `POST /api/post/publish` 发帖接口 |
| Controller | `controller/UploadController.java` | `POST /upload/image` `/upload/video` 文件上传 |
| Controller | `controller/FileProxyController.java` | `GET /files/**` MinIO 文件代理 |
| Service | `service/PostService.java` + `impl/PostServiceImpl.java` | 发帖主逻辑：校验 → 写DB → 发Kafka |
| Service | `service/MinioService.java` | MinIO 文件上传 (按日期分目录，UUID 命名) |
| Service | `service/VideoTranscodeService.java` | FFmpeg 视频转码 (H.265→H.264) + 封面截帧 |
| Service | `service/KafkaConsumerService.java` | 监听 `content-review` topic，审核 + 触发 Fan-out |
| Mapper | `mapper/PostMapper.java` + `.xml` | insertContent / updateState / getNextItemId |
| Mapper | `mapper/ContentImageMapper.java` + `.xml` | 批量插入图片记录 |
| Mapper | `mapper/ContentVideoMapper.java` + `.xml` | 插入视频记录 |
| 实体 | `entity/dto/CreatePostDTO.java` | 发帖请求参数 (@Valid 校验) |
| 实体 | `entity/vo/CreatePostVO.java` | 发帖返回 { itemId } |
| 实体 | `entity/vo/UploadVO.java` | 上传返回 { url, objectName, coverUrl } |

## 发帖链路

```
[第一步] 前端上传文件 → UploadController → MinioService
  ├─ 图片: images/20260614/{uuid}.jpg
  └─ 视频: videos/20260614/{uuid}.mp4 (+ 自动转码 + 截封面)
  → 返回 { url, objectName, coverUrl }

[第二步] 前端 POST /api/post/publish
  │  Body: { contentType, title, description, visibility, imageUrls[], videoUrl, coverUrl }
  │
  ▼
PostServiceImpl.postContent()  ← @Transactional
  ├─ 1. 业务校验 (图文至少1张最多3张, 视频必传URL)
  ├─ 2. SELECT MAX(item_id)+1 生成新 ID (非自增, 方便分库分表)
  ├─ 3. INSERT item_info (status=0 待审核, publish_time=null)
  ├─ 4. batch INSERT image_info 或 INSERT video_info
  └─ 5. KafkaTemplate.send("content-review", itemId)
        → 发布接口立即返回 { itemId }，不等审核结果
```

## 数据库表

- `item_info` — 内容主表，PK: item_id (非自增), IDX: creator_id
- `image_info` — 图片表，FK: item_id + version + status
- `video_info` — 视频表，FK: creator_id + status
- `item_text` — 文本版本表，UK: item_id + version

详见 `docs/table.md`

## 设计决策

- 文件先上传后发帖 (两步分离)，前端拿到 URL 后再提交帖子
- item_id 不用自增，用 MAX+1 手动生成，方便以后分库分表
- 发布和审核通过 Kafka 异步解耦，发布接口快速返回
- 视频上传时自动检测编码，H.265 转 H.264 (Chrome 不支持 HEVC)
- 转码后 MP4 加 +faststart (moov atom 前置，支持边下边播)
- 视频自动截第 1 秒画面作为封面
- FileProxyController 代理 MinIO 文件访问，解决前端跨域/鉴权问题
