package org.sdb.buzzfeed.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feed 流每条内容的返回 VO
 * 包含内容基础信息 + 发布者用户信息 + 媒体资源
 */
@Data
public class FeedItemVO {

    /** 内容唯一标识 */
    private Long itemId;

    /** 发布者用户ID */
    private Long creatorId;

    /** 内容类型：0纯文本 1图文 2视频 3长文 */
    private Integer itemType;

    /** 标题 */
    private String title;

    /** 摘要/正文 */
    private String summary;

    /** 发布时间 */
    private LocalDateTime publishTime;

    // ========== 用户信息 ==========

    /** 用户名 */
    private String username;

    /** 显示昵称 */
    private String displayName;

    /** 头像 */
    private String avatar;

    // ========== 媒体资源 ==========

    /** 图片URL列表（图文类型，最多3张） */
    private List<String> imageUrls;

    /** 视频URL（视频类型） */
    private String videoUrl;

    /** 视频封面URL */
    private String videoCoverUrl;

    /** 视频时长（秒） */
    private Integer videoDuration;
}
