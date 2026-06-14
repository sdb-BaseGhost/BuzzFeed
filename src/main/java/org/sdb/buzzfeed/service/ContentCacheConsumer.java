package org.sdb.buzzfeed.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.utils.RedisFeedHelper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Canal binlog 消费者：监听 item_info 表变更，维护 Redis 缓存
 *
 * Canal Kafka Adapter 推送的 JSON 格式：
 * {
 *   "database": "buzzfeed",
 *   "table": "item_info",
 *   "type": "INSERT" / "UPDATE" / "DELETE",
 *   "data": [ { "item_id": ..., ... } ],
 *   "old":  [ { "status": 0, ... } ]
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCacheConsumer {

    private final ContentCacheService contentCacheService;
    private final RedisFeedHelper redisFeedHelper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @KafkaListener(topics = "content-change", groupId = "content-cache-group")
    public void onContentChange(String message) {
        log.debug("收到 Canal 消息: {}", message);
        JsonNode root;
        try {
            root = objectMapper.readTree(message);
        } catch (Exception e) {
            log.warn("Canal 消息解析失败，丢弃: {}", message, e);
            return;
        }

        String table = root.path("table").asText("");
        if (!"item_info".equals(table)) return;

        String type = root.path("type").asText("");
        JsonNode dataArray = root.path("data");
        JsonNode oldArray  = root.path("old");

        if (!dataArray.isArray() || dataArray.isEmpty()) return;

        switch (type) {
            case "INSERT" -> handleInsert(dataArray);
            case "UPDATE" -> handleUpdate(dataArray, oldArray);
            case "DELETE" -> handleDelete(dataArray);
            default -> log.debug("忽略 Canal 事件类型: {}", type);
        }
    }

    private void handleInsert(JsonNode dataArray) {
        for (JsonNode row : dataArray) {
            Content content = parseContent(row);
            if (content == null) continue;
            // 仅已发布状态的内容写入缓存和 outbox
            if (content.getStatus() != null && content.getStatus() == 1) {
                contentCacheService.cacheContent(content);
                redisFeedHelper.addOutboxItem(content.getCreatorId(), content.getItemId(), content.getPublishTime());
                log.info("Canal INSERT -> 缓存+outbox: contentId={}", content.getItemId());
            }
        }
    }

    private void handleUpdate(JsonNode dataArray, JsonNode oldArray) {
        for (int i = 0; i < dataArray.size(); i++) {
            JsonNode row = dataArray.get(i);
            Content content = parseContent(row);
            if (content == null) continue;

            // 检查旧状态
            Integer oldStatus = null;
            if (oldArray.isArray() && i < oldArray.size()) {
                JsonNode oldRow = oldArray.get(i);
                if (oldRow.has("status")) {
                    oldStatus = oldRow.get("status").asInt();
                }
            }

            if (content.getStatus() != null && content.getStatus() == 1) {
                // 当前已发布 -> 写入缓存 + outbox
                contentCacheService.cacheContent(content);
                redisFeedHelper.addOutboxItem(content.getCreatorId(), content.getItemId(), content.getPublishTime());
                log.info("Canal UPDATE -> 缓存+outbox: contentId={}", content.getItemId());
            } else if (oldStatus != null && oldStatus == 1) {
                // 从已发布变为非已发布（下架/删除）-> 清除缓存 + outbox
                contentCacheService.evictContent(content.getItemId());
                redisFeedHelper.removeOutboxItem(content.getCreatorId(), content.getItemId());
                log.info("Canal UPDATE -> 清除缓存+outbox: contentId={}", content.getItemId());
            }
        }
    }

    private void handleDelete(JsonNode dataArray) {
        for (JsonNode row : dataArray) {
            Content content = parseContent(row);
            if (content == null) continue;
            contentCacheService.evictContent(content.getItemId());
            if (content.getCreatorId() != null) {
                redisFeedHelper.removeOutboxItem(content.getCreatorId(), content.getItemId());
            }
            log.info("Canal DELETE -> 清除缓存+outbox: contentId={}", content.getItemId());
        }
    }

    /**
     * 解析 Canal JSON 行数据为 Content 对象
     * 字段名是下划线格式（item_id, creator_id, ...）
     */
    private Content parseContent(JsonNode row) {
        try {
            Content content = new Content();
            content.setItemId(row.path("item_id").asLong());
            content.setCreatorId(row.path("creator_id").asLong());
            content.setItemType(row.path("item_type").asInt());
            content.setTitle(row.path("title").asText(null));
            content.setSummary(row.path("summary").asText(null));
            content.setStatus(row.path("status").asInt());

            String publishTimeStr = row.path("publish_time").asText(null);
            if (publishTimeStr != null && !publishTimeStr.isEmpty() && !"null".equals(publishTimeStr)) {
                content.setPublishTime(LocalDateTime.parse(publishTimeStr, DT_FMT));
            }
            return content;
        } catch (Exception e) {
            log.warn("Canal 行数据解析失败: {}", row, e);
            return null;
        }
    }
}