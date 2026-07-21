package org.sdb.buzzfeed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.FanoutSubTask;
import org.sdb.buzzfeed.mapper.InboxMapper;
import org.sdb.buzzfeed.utils.RedisFeedHelper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutExecutor {

    private final InboxMapper inboxMapper;
    private final RedisFeedHelper redisFeedHelper;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "fanout-subtask",
        groupId = "fanout-executor-group",
        containerFactory = "executorListenerContainerFactory"
    )
    public void onSubTask(String message) {
        FanoutSubTask task;
        try {
            task = objectMapper.readValue(message, FanoutSubTask.class);
        } catch (Exception e) {
            log.error("子任务反序列化失败，丢弃: {}", message, e);
            return;
        }

        log.info("Executor 收到子任务: contentId={}, taskId={}/{}, 粉丝数={}",
                 task.getContentId(), task.getTaskId(), task.getTotalShards(), task.getFanIds().size());

        // 1. 批量写入 MySQL inbox（INSERT IGNORE，唯一键保证幂等）
        try {
            inboxMapper.insertInbox(task.getFanIds(), task.getContentId(), task.getPublishTime());
            log.info("MySQL inbox 写入成功: contentId={}, taskId={}, 粉丝数={}",
                     task.getContentId(), task.getTaskId(), task.getFanIds().size());
        } catch (Exception e) {
            log.error("MySQL inbox 写入失败，等待 Kafka 重投递: contentId={}, taskId={}",
                      task.getContentId(), task.getTaskId(), e);
            throw e;
        }

        // 2. 批量写入 Redis inbox ZSET（Pipeline，幂等）
        try {
            redisFeedHelper.batchAddInbox(task.getFanIds(), task.getContentId(), task.getPublishTime());
            log.info("Redis inbox 写入成功: contentId={}, taskId={}",
                     task.getContentId(), task.getTaskId());
        } catch (Exception e) {
            log.warn("Redis inbox 写入失败，不影响主流程: contentId={}, taskId={}, error={}",
                     task.getContentId(), task.getTaskId(), e.getMessage());
        }

        log.info("Executor 子任务完成: contentId={}, taskId={}/{}",
                 task.getContentId(), task.getTaskId(), task.getTotalShards());
    }
}
