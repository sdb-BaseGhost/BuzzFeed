package org.sdb.buzzfeed.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Feed {
    /**
     * 一次获取的feed流数量
     */
    private int num;

    /**
     * 最后一条feed流的id
     */
    private Long contentId;
    /**
     * 最后一条feed流的时间
     */
    private LocalDateTime lastTime;

    /**
     * 判断是操作类型：0是下拉，1是上滑
     */
    private Integer type;

    /**
     * 发起请求的用户id
     */
    private Long userId;
}
