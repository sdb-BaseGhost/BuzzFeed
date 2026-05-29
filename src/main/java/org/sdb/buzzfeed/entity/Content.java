package org.sdb.buzzfeed.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Content {
    /** 内容唯一标识（主键ID） */
    private int contentId;

    /** 发布时间（内容首次创建的时间） */
    private LocalDateTime publishTime;

    /** 更新时间（内容修改后的最新时间） */
    private LocalDateTime updateTime;

    /** 发布者用户ID（关联用户表的主键） */
    private Long userId;

    /** 简短文本（例如内容摘要、短句、标题等） */
    private String shortText;

    /** 长文本（内容主体，可能是一段较长的文字） */
    private String longText;

    /** 图片资源URL（如果内容带图片则存储其路径或URL） */
    private String photo;

    /** 视频资源URL（如果内容带视频则存储其路径或URL） */
    private String video;

    /** 音频资源URL（如果内容带音乐或语音则存储其路径或URL） */
    private String music;

    /** 内容版本号（用于版本控制或乐观锁，例如编辑时使用） */
    private String version;

    /** 内容状态（如：draft草稿、published已发布、deleted已删除等） */
    private String status;
}
