package org.sdb.buzzfeed.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fan-out 推送消息体
 * Kafka Topic: feed-fanout-request
 * Key: creatorId (保证同一创作者的消息有序)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FanoutMessage {
    /** 内容ID */
    private Long contentId;
    /** 创作者ID */
    private Long creatorId;
    /** 发布时间 */
    private LocalDateTime publishTime;
}
