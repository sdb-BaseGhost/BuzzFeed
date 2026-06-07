package org.sdb.buzzfeed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.FanoutMessage;
import org.sdb.buzzfeed.mapper.PostMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final PostMapper postMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String FANOUT_TOPIC = "feed-fanout-request";

    @KafkaListener(topics = "content-review", groupId = "content-review-group")
    public void consumeContentReview(String contentIdStr) {
        log.info("收到审核消息，contentId: {}", contentIdStr);

        Long contentId = Long.parseLong(contentIdStr);
        int reviewState = simulateReview(contentId);

        if (reviewState == 1) {
            // 审核通过：更新状态 + 写入 publish_time
            postMapper.approveContent(contentId, 1);
            log.info("contentId={} 审核通过，已更新状态和发布时间", contentId);

            // 查出完整内容信息
            Content content = postMapper.selectById(contentId);
            if (content == null) {
                log.error("contentId={} 审核通过但内容不存在", contentId);
                return;
            }

            // 发送 Fan-out 请求到 feed-fanout-request topic
            try {
                FanoutMessage fanoutMsg = new FanoutMessage(
                    content.getItemId(),
                    content.getCreatorId(),
                    content.getPublishTime()
                );
                String json = objectMapper.writeValueAsString(fanoutMsg);
                // Key = creatorId，保证同一创作者的消息落到同一分区、有序消费
                kafkaTemplate.send(FANOUT_TOPIC, String.valueOf(content.getCreatorId()), json);
                log.info("已发送 Fan-out 请求: contentId={}, creatorId={}",
                         contentId, content.getCreatorId());
            } catch (Exception e) {
                log.error("发送 Fan-out 请求失败: contentId={}", contentId, e);
                // Fan-out 发送失败不影响审核结果，内容已经是发布状态
                // 后续可通过补偿任务重新触发 Fan-out
            }
        } else {
            // 审核拒绝：仅更新状态
            postMapper.updateState(contentId, reviewState);
            log.info("contentId={} 审核拒绝", contentId);
        }
    }

    private int simulateReview(Long contentId) {
        return 1;  // MVP 阶段全部通过
    }
}
