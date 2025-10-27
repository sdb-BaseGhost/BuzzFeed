package org.sdb.buzzfeed.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserFollow {
    private Long id;            // 主键ID
    private Long userId;        // 关注人ID（谁去关注别人）
    private Long followUserId;  // 被关注人ID（谁被关注）
    private LocalDateTime createTime;    // 关注时间
}

