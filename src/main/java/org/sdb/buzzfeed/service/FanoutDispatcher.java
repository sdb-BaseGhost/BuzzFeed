package org.sdb.buzzfeed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.FanoutMessage;
import org.sdb.buzzfeed.entity.FanoutSubTask;
import org.sdb.buzzfeed.mapper.FollowMapper;
import org.sdb.buzzfeed.utils.RedisFeedHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutDispatcher {

    private final FollowMapper followMapper;
    private final RedisFeedHelper redisFeedHelper;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${feed.big-v-threshold:2}")
    private int bigVThreshold;

    @Value("${feed.shard-size:5000}")
    private int shardSize;

    private static final String SUBTASK_TOPIC = "fanout-subtask";

    @KafkaListener(
        topics = "feed-fanout-request",
        groupId = "fanout-dispatcher-group",
        containerFactory = "dispatcherListenerContainerFactory"
    )
    public void onFanoutRequest(String message) {
        log.info("Dispatcher 收到 Fan-out 请求: {}", message);

        FanoutMessage msg;
        try {
            msg = objectMapper.readValue(message, FanoutMessage.class);
        } catch (Exception e) {
            log.error("Fan-out 消息反序列化失败，丢弃: {}", message, e);
            return;
        }

        // 1. 拉取粉丝列表
        List<Long> fans = followMapper.selectFollowerUserIds(msg.getCreatorId());
        if (fans == null || fans.isEmpty()) {
            log.info("创作者 {} 没有粉丝，跳过", msg.getCreatorId());
            return;
        }

        // 2. 判断是否是大V，大V只推活跃粉丝
        Long fansCount = followMapper.countFollowers(msg.getCreatorId());
        boolean isBigV = fansCount != null && fansCount > bigVThreshold;

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

        // 3. 粉丝列表排序（保证分片确定性）
        Collections.sort(targetFans);

        // 4. 动态分片
        int totalShards = (int) Math.ceil((double) targetFans.size() / shardSize);
        log.info("Fan-out 分片: contentId={}, 总粉丝={}, 分片大小={}, 总分片数={}",
                 msg.getContentId(), targetFans.size(), shardSize, totalShards);

        // 5. 逐片发送子任务
        for (int i = 0; i < totalShards; i++) {
            int fromIndex = i * shardSize;
            int toIndex = Math.min(fromIndex + shardSize, targetFans.size());
            List<Long> shardFans = targetFans.subList(fromIndex, toIndex);

            FanoutSubTask subTask = new FanoutSubTask(
                msg.getContentId(),
                i + 1,
                totalShards,
                msg.getPublishTime(),
                shardFans
            );

            try {
                String json = objectMapper.writeValueAsString(subTask);
                kafkaTemplate.send(SUBTASK_TOPIC, String.valueOf(msg.getContentId()), json);
            } catch (Exception e) {
                log.error("子任务发送失败: contentId={}, taskId={}", msg.getContentId(), i + 1, e);
            }
        }

        log.info("Dispatcher 完成分片分发: contentId={}, 共{}个子任务",
                 msg.getContentId(), totalShards);
    }

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
