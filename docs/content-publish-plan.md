# 内容发布功能 - 执行计划

## 目标
实现内容发布 + MinIO文件上传 + Kafka异步审核的完整链路。

## 技术栈确认
- Spring Boot 3.5.6 + MyBatis + MySQL + Redis + Kafka
- MinIO（本地Docker，官方Java SDK 8.5.7）
- Kafka（本地Docker，localhost:9092）

---

## 第一步：添加MinIO依赖

**文件：pom.xml**

在 dependencies 中新增：
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

---

## 第二步：配置 application.yml

**文件：src/main/resources/application.yml**

在已有配置末尾追加 MinIO 配置：
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: buzzfeed
```

Kafka 配置已有，无需改 yml。

---

## 第三步：新增 MinIO 配置类

**新建文件：src/main/java/org/sdb/buzzfeed/config/MinioConfig.java**

```java
package org.sdb.buzzfeed.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

---

## 第四步：新增 MinioService

**新建文件：src/main/java/org/sdb/buzzfeed/service/MinioService.java**

```java
package org.sdb.buzzfeed.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String upload(MultipartFile file, String directory) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = directory + "/" + dateStr + "/" + UUID.randomUUID() + ext;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        return objectName;
    }

    public String getFileUrl(String objectName) {
        return endpoint + "/" + bucketName + "/" + objectName;
    }
}
```

---

## 第五步：修改 Content 实体

**修改文件：src/main/java/org/sdb/buzzfeed/entity/Content.java**

新增字段：
```java
private String title;           // 标题
```

---

## 第六步：新增 ContentImage 实体

**新建文件：src/main/java/org/sdb/buzzfeed/entity/ContentImage.java**

```java
package org.sdb.buzzfeed.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentImage {
    private Long id;
    private Long contentId;
    private String imagePath;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
```

---

## 第七步：新增 ContentVideo 实体

**新建文件：src/main/java/org/sdb/buzzfeed/entity/ContentVideo.java**

```java
package org.sdb.buzzfeed.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentVideo {
    private Long id;
    private Long contentId;
    private String videoPath;
    private String coverPath;   // 本次先传 null
    private Integer duration;
    private Long fileSize;
    private LocalDateTime createdAt;
}
```

---

## 第八步：新增 ContentImageMapper

**新建文件：src/main/java/org/sdb/buzzfeed/mapper/ContentImageMapper.java**

```java
package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.ContentImage;
import java.util.List;

@Mapper
public interface ContentImageMapper {
    void batchInsert(@Param("list") List<ContentImage> images);
}
```

**新建文件：src/main/resources/mapper/ContentImageMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.sdb.buzzfeed.mapper.ContentImageMapper">

    <insert id="batchInsert" parameterType="java.util.List">
        INSERT INTO content_image (content_id, image_path, sort_order, created_at)
        VALUES
        <foreach collection="list" item="img" separator=",">
            (#{img.contentId}, #{img.imagePath}, #{img.sortOrder}, NOW())
        </foreach>
    </insert>

</mapper>
```

---

## 第九步：新增 ContentVideoMapper

**新建文件：src/main/java/org/sdb/buzzfeed/mapper/ContentVideoMapper.java**

```java
package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.sdb.buzzfeed.entity.ContentVideo;

@Mapper
public interface ContentVideoMapper {
    void insert(ContentVideo video);
}
```

**新建文件：src/main/resources/mapper/ContentVideoMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.sdb.buzzfeed.mapper.ContentVideoMapper">

    <insert id="insert" parameterType="org.sdb.buzzfeed.entity.ContentVideo"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO content_video (content_id, video_path, cover_path, duration, file_size, created_at)
        VALUES (#{contentId}, #{videoPath}, #{coverPath}, #{duration}, #{fileSize}, NOW())
    </insert>

</mapper>
```

---

## 第十步：修改 postMapper

**修改文件：src/main/java/org/sdb/buzzfeed/mapper/postMapper.java**

完整替换为：
```java
package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.Content;

@Mapper
public interface postMapper {
    void insertContent(Content content);   // useGeneratedKeys 回填 id
    void updateState(@Param("id") Long id, @Param("state") Integer state);
}
```

**修改文件：src/main/resources/mapper/postMapper.xml**

完整替换为：
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.sdb.buzzfeed.mapper.postMapper">

    <insert id="insertContent" parameterType="org.sdb.buzzfeed.entity.Content"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO content (content_type, user_id, title, description, visibility, state, is_top, is_original, create_time, update_time)
        VALUES (#{contentType}, #{userId}, #{title}, #{description}, #{visibility}, #{state}, 0, 1, NOW(), NOW())
    </insert>

    <update id="updateState">
        UPDATE content SET state = #{state}, update_time = NOW() WHERE id = #{id}
    </update>

</mapper>
```

---

## 第十一步：修改 PostController

**修改文件：src/main/java/org/sdb/buzzfeed/controller/PostController.java**

完整替换为：
```java
package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.service.PostService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/post")
    public Result post(
            @RequestParam("contentType") Integer contentType,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "visibility", defaultValue = "1") Integer visibility,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "video", required = false) MultipartFile video
    ) {
        return Result.success(postService.postContent(contentType, title, description, visibility, images, video));
    }
}
```

---

## 第十二步：修改 PostService 接口

**修改文件：src/main/java/org/sdb/buzzfeed/service/PostService.java**

完整替换为：
```java
package org.sdb.buzzfeed.service;

import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    Object postContent(Integer contentType, String title, String description,
                       Integer visibility, MultipartFile[] images, MultipartFile video);
}
```

---

## 第十三步：核心 - 重写 PostServiceImpl

**修改文件：src/main/java/org/sdb/buzzfeed/service/impl/PostServiceImpl.java**

完整替换为：
```java
package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.ContentImage;
import org.sdb.buzzfeed.entity.ContentVideo;
import org.sdb.buzzfeed.mapper.ContentImageMapper;
import org.sdb.buzzfeed.mapper.ContentVideoMapper;
import org.sdb.buzzfeed.mapper.postMapper;
import org.sdb.buzzfeed.service.MinioService;
import org.sdb.buzzfeed.service.PostService;
import org.sdb.buzzfeed.utils.UserContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final postMapper postMapper;
    private final ContentImageMapper contentImageMapper;
    private final ContentVideoMapper contentVideoMapper;
    private final MinioService minioService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String REVIEW_TOPIC = "content-review";

    @Override
    @Transactional
    public Object postContent(Integer contentType, String title, String description,
                               Integer visibility, MultipartFile[] images, MultipartFile video) {

        // 1. 参数校验
        if (contentType == null || title == null || title.isBlank()) {
            throw new RuntimeException("contentType 和 title 不能为空");
        }

        if (contentType == 1) {
            if (images == null || images.length == 0) {
                throw new RuntimeException("图文类型至少上传1张图片");
            }
            if (images.length > 3) {
                throw new RuntimeException("最多上传3张图片");
            }
        }

        if (contentType == 2) {
            if (video == null || video.isEmpty()) {
                throw new RuntimeException("视频类型必须上传视频");
            }
        }

        // 2. 构建 Content
        Content content = new Content();
        content.setContentType(contentType);
        content.setTitle(title);
        content.setDescription(description);
        content.setVisibility(visibility);
        content.setState(0);   // 待审核
        content.setUserId(UserContext.getUserId());

        // 3. 插入 content 表
        postMapper.insertContent(content);
        Long contentId = content.getId();

        // 4. 上传文件到 MinIO 并落库
        try {
            if (contentType == 1 && images != null) {
                List<ContentImage> imageList = new ArrayList<>();
                for (int i = 0; i < images.length; i++) {
                    String objectName = minioService.upload(images[i], "images");
                    ContentImage img = new ContentImage();
                    img.setContentId(contentId);
                    img.setImagePath(objectName);
                    img.setSortOrder(i + 1);
                    imageList.add(img);
                }
                contentImageMapper.batchInsert(imageList);
            }

            if (contentType == 2 && video != null) {
                String objectName = minioService.upload(video, "videos");
                ContentVideo cv = new ContentVideo();
                cv.setContentId(contentId);
                cv.setVideoPath(objectName);
                cv.setCoverPath(null);
                cv.setDuration(null);
                cv.setFileSize(video.getSize());
                contentVideoMapper.insert(cv);
            }
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }

        // 5. 发送 Kafka 审核消息
        kafkaTemplate.send(REVIEW_TOPIC, String.valueOf(contentId));

        return contentId;
    }
}
```

---

## 第十四步：修改 KafkaConsumerService

**修改文件：src/main/java/org/sdb/buzzfeed/service/KafkaConsumerService.java**

完整替换为：
```java
package org.sdb.buzzfeed.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.mapper.postMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final postMapper postMapper;

    @KafkaListener(topics = "content-review", groupId = "content-review-group")
    public void consumeContentReview(String contentIdStr) {
        log.info("收到审核消息，contentId: {}", contentIdStr);

        Long contentId = Long.parseLong(contentIdStr);
        int reviewState = simulateReview(contentId);
        postMapper.updateState(contentId, reviewState);

        if (reviewState == 1) {
            log.info("contentId={} 审核通过", contentId);
            // TODO: 审核通过后可发送 feed 推送消息
        } else {
            log.info("contentId={} 审核拒绝", contentId);
        }
    }

    private int simulateReview(Long contentId) {
        return 1;  // MVP 阶段全部通过
    }
}
```

---

## 文件变更总结

| 序号 | 操作 | 文件路径 |
|------|------|----------|
| 1 | 修改 | pom.xml |
| 2 | 修改 | src/main/resources/application.yml |
| 3 | 新建 | src/main/java/org/sdb/buzzfeed/config/MinioConfig.java |
| 4 | 新建 | src/main/java/org/sdb/buzzfeed/service/MinioService.java |
| 5 | 修改 | src/main/java/org/sdb/buzzfeed/entity/Content.java |
| 6 | 新建 | src/main/java/org/sdb/buzzfeed/entity/ContentImage.java |
| 7 | 新建 | src/main/java/org/sdb/buzzfeed/entity/ContentVideo.java |
| 8 | 新建 | src/main/java/org/sdb/buzzfeed/mapper/ContentImageMapper.java |
| 9 | 新建 | src/main/resources/mapper/ContentImageMapper.xml |
| 10 | 新建 | src/main/java/org/sdb/buzzfeed/mapper/ContentVideoMapper.java |
| 11 | 新建 | src/main/resources/mapper/ContentVideoMapper.xml |
| 12 | 修改 | src/main/java/org/sdb/buzzfeed/mapper/postMapper.java |
| 13 | 修改 | src/main/resources/mapper/postMapper.xml |
| 14 | 修改 | src/main/java/org/sdb/buzzfeed/controller/PostController.java |
| 15 | 修改 | src/main/java/org/sdb/buzzfeed/service/PostService.java |
| 16 | 修改 | src/main/java/org/sdb/buzzfeed/service/impl/PostServiceImpl.java |
| 17 | 修改 | src/main/java/org/sdb/buzzfeed/service/KafkaConsumerService.java |

---

## 数据库前置条件

确保 MySQL 中以下表已按 docs/table.md 建好：
- content（需包含 title 字段）
- content_image
- content_video

---

## 执行顺序建议

1. pom.xml 加依赖 -> mvn 下载
2. application.yml 加 MinIO 配置
3. 新建 MinioConfig + MinioService
4. 新建/修改实体层（Content、ContentImage、ContentVideo）
5. 新建/修改 Mapper 层（Java + XML）
6. 修改 PostController -> PostService 接口 -> PostServiceImpl
7. 修改 KafkaConsumerService
8. 启动项目验证

## 验证方式

启动项目后，用 curl 测试：

```bash
curl -X POST http://localhost:8080/post \
  -F 'contentType=1' \
  -F 'title=测试标题' \
  -F 'description=测试描述' \
  -F 'visibility=1' \
  -F 'images=@/path/to/image1.jpg' \
  -F 'images=@/path/to/image2.jpg' \
  -H 'Authorization: Bearer <your-jwt-token>'
```

预期响应：
```json
{"code": 200, "msg": "success", "data": 1}
```

同时观察控制台应出现 Kafka 审核消息日志和审核通过日志。
