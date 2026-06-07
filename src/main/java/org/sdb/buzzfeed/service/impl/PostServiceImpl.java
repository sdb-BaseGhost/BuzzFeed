package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.ContentImage;
import org.sdb.buzzfeed.entity.ContentVideo;
import org.sdb.buzzfeed.entity.dto.CreatePostDTO;
import org.sdb.buzzfeed.entity.vo.CreatePostVO;
import org.sdb.buzzfeed.mapper.ContentImageMapper;
import org.sdb.buzzfeed.mapper.ContentVideoMapper;
import org.sdb.buzzfeed.mapper.PostMapper;
import org.sdb.buzzfeed.service.PostService;
import org.sdb.buzzfeed.utils.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final ContentImageMapper contentImageMapper;
    private final ContentVideoMapper contentVideoMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    private static final String REVIEW_TOPIC = "content-review";

    @Override
    @Transactional
    public CreatePostVO postContent(CreatePostDTO dto) {
        Integer contentType = dto.getContentType();
        List<String> imageUrls = dto.getImageUrls();
        String videoUrl = dto.getVideoUrl();

        // 1. 业务规则校验（@Valid 已覆盖基础字段校验）
        if (contentType == 1) {
            if (imageUrls == null || imageUrls.isEmpty()) {
                throw new RuntimeException("图文类型至少上传1张图片");
            }
            if (imageUrls.size() > 3) {
                throw new RuntimeException("最多上传3张图片");
            }
        }

        if (contentType == 2) {
            if (videoUrl == null || videoUrl.isBlank()) {
                throw new RuntimeException("视频类型必须上传视频");
            }
        }

        // 2. 生成 item_id（item_info.item_id 非自增）
        Long itemId = postMapper.getNextItemId();

        // 3. 构建 Content 并插入 item_info
        Content content = new Content();
        content.setItemId(itemId);
        content.setItemType(contentType);
        content.setTitle(dto.getTitle());
        content.setSummary(dto.getDescription());
        content.setVisibility(dto.getVisibility());
        content.setStatus(0);   // 待审核
        content.setCreatorId(UserContext.getUserId());

        postMapper.insertContent(content);

        // 4. 根据已上传的 URL 落库（文件已在 UploadController 阶段传到 MinIO）
        if (contentType == 1 && imageUrls != null) {
            List<ContentImage> imageList = new ArrayList<>();
            String prefix = minioEndpoint + "/" + bucketName + "/";
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                ContentImage img = new ContentImage();
                img.setItemId(itemId);
                img.setImageUri(url.startsWith(prefix) ? url.substring(prefix.length()) : url);
                img.setSortOrder(i + 1);
                imageList.add(img);
            }
            contentImageMapper.batchInsert(imageList);
        }

        if (contentType == 2 && videoUrl != null) {
            String prefix = minioEndpoint + "/" + bucketName + "/";
            String objectName = videoUrl.startsWith(prefix) ? videoUrl.substring(prefix.length()) : videoUrl;
            ContentVideo cv = new ContentVideo();
            cv.setItemId(itemId);
            cv.setCreatorId(UserContext.getUserId());
            cv.setBucketName(bucketName);
            cv.setObjectName(objectName);
            cv.setVideoUrl(videoUrl);
            cv.setDuration(0);
            cv.setFileSize(0L);
            contentVideoMapper.insert(cv);
        }

        // 5. 发送 Kafka 审核消息
        kafkaTemplate.send(REVIEW_TOPIC, String.valueOf(itemId));

        return new CreatePostVO(itemId);
    }
}

