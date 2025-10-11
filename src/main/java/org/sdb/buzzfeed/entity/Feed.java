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
    private String contentId;
    /**
     * 最后一条feed流的时间
     */
    private LocalDateTime lastTime;
}
