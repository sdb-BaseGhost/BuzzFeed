package org.sdb.buzzfeed.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系实体，映射 following / follower 表共用结构
 */
@Data
public class FollowRelation {
    private Long id;
    private Long fromUserId;     // 关注者ID
    private Long toUserId;       // 被关注者ID
    private Integer type;        // 关系状态：1-正在关注 2-取消关注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
