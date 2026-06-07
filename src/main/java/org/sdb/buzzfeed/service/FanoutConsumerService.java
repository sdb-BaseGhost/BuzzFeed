package org.sdb.buzzfeed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.FanoutMessage;
import org.sdb.buzzfeed.mapper.InboxMapper;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutConsumerService {

    private final UserMapper userMapper;
    private final InboxMapper inboxMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${feed.big-v-threshold:2}")
    private int bigVThreshold;

    private static final int BATCH_SIZE = 200;

    @KafkaListener(
        topics = "feed-fanout-request",
        groupId = "feed-fanout-group",
        containerFactory = "fanoutListenerContainerFactory"
    )
    public void onFanoutRequest(String message) {
        log.info("收到 Fan-out 请求: {}", message);

        FanoutMessage msg;
        try {
            msg = objectMapper.readValue(message, FanoutMessage.class);
        } catch (Exception e) {
            log.error("Fan-out 消息反序列化失败，丢弃: {}", message, e);
            throw new IllegalArgumentException("消息格式错误: " + message, e);
        }

        if (msg.getContentId() == null || msg.getCreatorId() == null) {
            log.warn("Fan-out 消息字段缺失，丢弃: {}", message);
            throw new IllegalArgumentException("消息字段缺失: " + message);
        }

        // 1. 拉取粉丝列表
        List<Long> fans = userMapper.selectFollowersByUserId(msg.getCreatorId());
        if (fans == null || fans.isEmpty()) {
            log.info("创作者 {} 没有粉丝，跳过 Fan-out", msg.getCreatorId());
            return;
        }

        // 2. 判断是否是大V（粉丝数 > 阈值）
        Integer fansCount = userMapper.selectFansCount(msg.getCreatorId());
        boolean isBigV = fansCount != null && fansCount > bigVThreshold;

        // 3. 大V只推给活跃粉丝，非大V推给所有粉丝
        List<Long> targetFans;
        if (isBigV) {
            targetFans = filterActiveFans(fans);
            log.info("创作者 {} 是大V({}粉丝)，活跃粉丝 {} 人",
                     msg.getCreatorId(), fansCount, targetFans.size());
        } else {
            targetFans = fans;
            log.info("创作者 {} 非大V({}粉丝)，推送给所有粉丝",
                     msg.getCreatorId(), fans.size());
        }

        if (targetFans.isEmpty()) {
            log.info("创作者 {} 没有需要推送的粉丝，跳过", msg.getCreatorId());
            return;
        }

        // 4. 分批写入收件箱（每200条一批）
        for (int i = 0; i < targetFans.size(); i += BATCH_SIZE) {
            List<Long> batch = targetFans.subList(i, Math.min(i + BATCH_SIZE, targetFans.size()));
            inboxMapper.insertInbox(batch, msg.getContentId(), msg.getPublishTime());
            log.info("Fan-out 批量写入收件箱: contentId={}, 本批 {} 人, 进度 {}/{}",
                     msg.getContentId(), batch.size(),
                     Math.min(i + BATCH_SIZE, targetFans.size()), targetFans.size());
        }

        log.info("Fan-out 完成: contentId={}, 总共推送 {} 人", msg.getContentId(), targetFans.size());
    }

    /**
     * 从粉丝列表中过滤出活跃用户（Redis key存在即为近7天活跃）
     */
    private List<Long> filterActiveFans(List<Long> fans) {
        List<Long> activeFans = new ArrayList<>();
        for (Long fanId : fans) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("active:user:" + fanId))) {
                activeFans.add(fanId);
            }
        }
        return activeFans;
    }
}
