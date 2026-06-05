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