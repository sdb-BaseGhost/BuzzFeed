package org.sdb.buzzfeed.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentVideo {
    /** 对应 video_info.id（自增主键） */
    private Long id;
    /** 对应 video_info.item_id */
    private Long itemId;
    /** 对应 video_info.sort_order */
    private Integer sortOrder;
    /** 对应 video_info.version，默认1 */
    private Integer version;
    /** 对应 video_info.creator_id */
    private Long creatorId;
    /** 对应 video_info.bucket_name */
    private String bucketName;
    /** 对应 video_info.object_name */
    private String objectName;
    /** 对应 video_info.video_url */
    private String videoUrl;
    /** 对应 video_info.cover_url */
    private String coverUrl;
    /** 对应 video_info.duration */
    private Integer duration;
    /** 对应 video_info.file_size */
    private Long fileSize;
    /** 对应 video_info.status，默认0（转码中） */
    private Integer status;
    /** 对应 video_info.create_time */
    private LocalDateTime createTime;
    /** 对应 video_info.update_time */
    private LocalDateTime updateTime;
}
