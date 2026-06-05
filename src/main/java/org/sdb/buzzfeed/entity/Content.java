package org.sdb.buzzfeed.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Content {
    /** 内容唯一标识（对应 item_info.item_id，非自增） */
    private Long itemId;

    /** 发布者用户ID（对应 item_info.creator_id） */
    private Long creatorId;

    /** 内容类型：0纯文本 1图文 2视频 3长文（对应 item_info.item_type） */
    private Integer itemType;

    /** 标题（对应 item_info.title） */
    private String title;

    /** 摘要（对应 item_info.summary） */
    private String summary;

    /** 可见范围：0私密 1好友 2粉丝 3公开（对应 item_info.visibility） */
    private Integer visibility;

    /** 状态：0待审核 1正常 2删除 3下架（对应 item_info.status） */
    private Integer status;

    /** 创建时间（对应 item_info.create_time） */
    private LocalDateTime createTime;

    /** 发布时间（对应 item_info.publish_time） */
    private LocalDateTime publishTime;

    /** 更新时间（对应 item_info.update_time） */
    private LocalDateTime updateTime;
}
