package org.sdb.buzzfeed.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentImage {
    /** 对应 image_info.id（自增主键） */
    private Long id;
    /** 对应 image_info.item_id */
    private Long itemId;
    /** 对应 image_info.image_uri */
    private String imageUri;
    /** 对应 image_info.sort_order */
    private Integer sortOrder;
    /** 对应 image_info.version，默认1 */
    private Integer version;
    /** 对应 image_info.status，默认1 */
    private Integer status;
    /** 对应 image_info.create_time */
    private LocalDateTime createTime;
    /** 对应 image_info.update_time */
    private LocalDateTime updateTime;
}
