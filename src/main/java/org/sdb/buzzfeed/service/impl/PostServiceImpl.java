package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.ContentImage;
import org.sdb.buzzfeed.entity.ContentVideo;
import org.sdb.buzzfeed.mapper.ContentImageMapper;
import org.sdb.buzzfeed.mapper.ContentVideoMapper;
import org.sdb.buzzfeed.mapper.PostMapper;
import org.sdb.buzzfeed.service.MinioService;
import org.sdb.buzzfeed.service.PostService;
import org.sdb.buzzfeed.utils.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final ContentImageMapper contentImageMapper;
    private final ContentVideoMapper contentVideoMapper;
    private final MinioService minioService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

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

        // 2. 生成 item_id（item_info.item_id 非自增）
        Long itemId = postMapper.getNextItemId();

        // 3. 构建 Content 并插入 item_info
        Content content = new Content();
        content.setItemId(itemId);
        content.setItemType(contentType);
        content.setTitle(title);
        content.setSummary(description);
        content.setVisibility(visibility);
        content.setStatus(0);   // 待审核
        content.setCreatorId(UserContext.getUserId());

        postMapper.insertContent(content);

        // 4. 上传文件到 MinIO 并落库
        try {
            if (contentType == 1 && images != null) {
                List<ContentImage> imageList = new ArrayList<>();
                for (int i = 0; i < images.length; i++) {
                    String objectName = minioService.upload(images[i], "images");
                    ContentImage img = new ContentImage();
                    img.setItemId(itemId);
                    img.setImageUri(objectName);
                    img.setSortOrder(i + 1);
                    imageList.add(img);
                }
                contentImageMapper.batchInsert(imageList);
            }

            if (contentType == 2 && video != null) {
                String objectName = minioService.upload(video, "videos");
                String videoUrl = minioEndpoint + "/" + bucketName + "/" + objectName;
                ContentVideo cv = new ContentVideo();
                cv.setItemId(itemId);
                cv.setCreatorId(UserContext.getUserId());
                cv.setBucketName(bucketName);
                cv.setObjectName(objectName);
                cv.setVideoUrl(videoUrl);
                cv.setDuration(0);
                cv.setFileSize(video.getSize());
                contentVideoMapper.insert(cv);
            }
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }

        // 5. 发送 Kafka 审核消息
        kafkaTemplate.send(REVIEW_TOPIC, String.valueOf(itemId));

        return itemId;
    }
}

