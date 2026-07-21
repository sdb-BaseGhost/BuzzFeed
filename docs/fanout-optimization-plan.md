# Fan-out 推送优化 — 两阶段架构实现计划

## 目标

将当前单阶段 Fan-out（1个消费者串行处理全部粉丝）改为两阶段架构：
- **阶段一 Dispatcher**：接收审核通过消息，查粉丝，动态分片，分发子任务
- **阶段二 Executor × N**：多个执行器并行消费子任务，批量写入 in
幂等性由 inbox 表唯一键 + INSERT IGNORE 保证，不需要额外进度表。

## 架构变更

```
之前:
  KafkaConsumerService → FanoutConsumerService (1个消费者, 3线程竞争消费)

之后:
  KafkaConsumerService → FanoutDispatcher (单实例, 动态分片)
                            ↓ Kafka("fanout-subtask")
                         FanoutExecutor × N (多实例并行消费)
```

## 设计决策（已确认）

| 决策点 | 选择 |
|--------|------|
| 粉丝分片策略 | 动态分片，每片 5000 粉丝 |
| 粉丝 ID 传递方式 | 放在 Kafka 消息里，executor 直接使用 |
| 进度追踪 | 不做，靠唯一键幂等 |
| 断点续传 | 不做，失败靠 Kafka 重投递 + INSERT IGNORE |
| 幂等保证 | inbox 表 UNIQUE KEY (user_id, content_id) |

---

## 第一步：数据库 — inbox 表加唯一键

**操作：** 在 MySQL 中执行

```sql
ALTER TABLE inbox ADD UNIQUE KEY uk_user_content (user_id, content_id);
```

**影响：** 防止重复推送同一内容到同一用户收件箱。后续 INSERT IGNORE 依赖此索引。

---

## 第二步：修改 InboxMapper.xml — INSERT → INSERT IGNORE

**文件：** `src/main/resources/mapper/InboxMapper.xml`

**改动：** `insertInbox` 的 SQL 语句加 `IGNORE`

```xml
<!-- 改前 -->
INSERT INTO inbox (user_id, content_id, publish_time)

<!-- 改后 -->
INSERT IGNORE INTO inbox (user_id, content_id, publish_time)
```

**原因：** executor 崩溃重试时，已插入的粉丝会被唯一键过滤，不报错。

---

## 第三步：新增 Kafka topic — fanout-subtask

**操作：** 在 Kafka 容器中执行

```bash
kafka-topics.sh --create \
  --topic fanout-subtask \
  --bootstrap-server kafka:9092 \
  --partitions 6 \
  --replication-factor 1
```

**分区数说明：** 6 个分区允许最多 6 个 executor 并行消费（同一 consumer group 内）。

---

## 第四步：新增子任务实体

**新建文件：** `src/main/java/org/sdb/buzzfeed/entity/FanoutSubTask.java`

```java
package org.sdb.buzzfeed.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fan-out 子任务：由 Dispatcher 分片后发送给 Executor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FanoutSubTask {
    private Long contentId;           // 内容ID
    private Integer taskId;           // 分片编号 (1, 2, 3...)
    private Integer totalShards;      // 总分片数
    private LocalDateTime publishTime; // 发布时间
    private List<Long> fanIds;        // 该分片的粉丝ID列表
}
```

---

## 第五步：新增 FanoutDispatcher

**新建文件：** `src/main/java/org/sdb/buzzfeed/service/FanoutDispatcher.java`

**职责：** 监听 `feed-fanout-request`，查粉丝，动态分片，发送子任务到 `fanout-subtask`

```java
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
                i + 1,          // taskId 从1开始
                totalShards,
                msg.getPublishTime(),
                shardFans
            );

            try {
                String json = objectMapper.writeValueAsString(subTask);
                // Key = contentId，保证同一内容的子任务落到同一分区、有序消费
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
```

**关键点：**
- `containerFactory = "dispatcherListenerContainerFactory"` — concurrency=1，单实例避免重复分片
- 粉丝排序 `Collections.sort(targetFans)` — 保证分片确定性，同一内容多次分片结果一致
- `shardSize` 通过配置 `feed.shard-size` 控制，默认 5000
- Kafka Key = contentId — 保证同一内容的子任务有序

---

## 第六步：新增 FanoutExecutor

**新建文件：** `src/main/java/org/sdb/buzzfeed/service/FanoutExecutor.java`

**职责：** 监听 `fanout-subtask`，批量写入 inbox（MySQL + Redis）

```java
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
            throw e; // 抛出异常让 Kafka 重试
        }

        // 2. 批量写入 Redis inbox ZSET（Pipeline，幂等）
        try {
            redisFeedHelper.batchAddInbox(task.getFanIds(), task.getContentId(), task.getPublishTime());
            log.info("Redis inbox 写入成功: contentId={}, taskId={}",
                     task.getContentId(), task.getTaskId());
        } catch (Exception e) {
            // Redis 写入失败不阻塞主流程，MySQL 是主存储
            log.warn("Redis inbox 写入失败，不影响主流程: contentId={}, taskId={}, error={}",
                     task.getContentId(), task.getTaskId(), e.getMessage());
        }

        log.info("Executor 子任务完成: contentId={}, taskId={}/{}",
                 task.getContentId(), task.getTaskId(), task.getTotalShards());
    }
}
```

**关键点：**
- `containerFactory = "executorListenerContainerFactory"` — concurrency 可配置，压测时调整
- MySQL 写入失败 → throw → Kafka 重投递 → INSERT IGNORE 幂等处理
- Redis 写入失败 → catch 住，不影响主流程（MySQL 是主存储）

---

## 第七步：修改 KafkaConfig — 新增 executor 工厂

**文件：** `src/main/java/org/sdb/buzzfeed/config/KafkaConfig.java`

**改动：**
1. 原 `fanoutListenerContainerFactory` 改名为 `dispatcherListenerContainerFactory`，concurrency 改为 1
2. 新增 `executorListenerContainerFactory`，concurrency 可配置

```java
package org.sdb.buzzfeed.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Value("${feed.executor-concurrency:3}")
    private int executorConcurrency;

    /**
     * Dispatcher 容器工厂：单线程，避免重复分片
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            dispatcherListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory,
                KafkaTemplate<String, String> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);  // 单实例

        FixedBackOff backOff = new FixedBackOff(1000L, 3L);
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * Executor 容器工厂：多线程并行消费子任务
     * 通过 feed.executor-concurrency 配置线程数
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            executorListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory,
                KafkaTemplate<String, String> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(executorConcurrency);  // 可配置

        FixedBackOff backOff = new FixedBackOff(1000L, 3L);
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
```

**application.yml 新增配置：**

```yaml
feed:
  big-v-threshold: 2
  shard-size: 5000
  executor-concurrency: 3
```

---

## 第八步：删除旧的 FanoutConsumerService

**删除文件：** `src/main/java/org/sdb/buzzfeed/service/FanoutConsumerService.java`

**原因：** 已被 FanoutDispatcher + FanoutExecutor 完全替代。

---

## 文件变更清单

| 序号 | 操作 | 文件路径 | 说明 |
|------|------|----------|------|
| 1 | SQL | 数据库 | ALTER TABLE inbox 加唯一键 |
| 2 | 修改 | `mapper/InboxMapper.xml` | INSERT → INSERT IGNORE |
| 3 | SQL | Kafka | 创建 fanout-subtask topic |
| 4 | 新建 | `entity/FanoutSubTask.java` | 子任务实体 |
| 5 | 新建 | `service/FanoutDispatcher.java` | 阶段一：分片分发 |
| 6 | 新建 | `service/FanoutExecutor.java` | 阶段二：执行写入 |
| 7 | 修改 | `config/KafkaConfig.java` | 新增 executor 工厂 |
| 8 | 修改 | `application.yml` | 新增 shard-size, executor-concurrency |
| 9 | 删除 | `service/FanoutConsumerService.java` | 旧代码清理 |

---

## 验证方式

1. 启动项目，确认无编译错误
2. 发帖 → 观察日志：
   - KafkaConsumerService: "审核通过"
   - FanoutDispatcher: "收到 Fan-out 请求" → "分片分发完成"
   - FanoutExecutor: "收到子任务" → "子任务完成"
3. 查 inbox 表确认粉丝已入库
4. Feed 流读取确认内容可见

---

## 压测时调整参数

```yaml
feed:
  shard-size: 5000        # 分片大小，可根据压测结果调整
  executor-concurrency: 3  # executor 线程数，压测时逐步增加
```

压测建议：
1. 先用 `executor-concurrency=1` 测单 executor 吞吐
2. 逐步增加到 3、5、10，观察 DB 连接池是否成为瓶颈
3. 关注 MySQL 写入 TPS、Kafka consumer lag、inbox 表行数增长速度