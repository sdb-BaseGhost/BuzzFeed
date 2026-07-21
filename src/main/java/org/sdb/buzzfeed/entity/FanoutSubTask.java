package org.sdb.buzzfeed.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fan-out 子任务：由 Dispatcher 分片后发送给 Executor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FanoutSubTask {
    private Long contentId;           // 内容ID
    private Integer taskId;           // 分片编号 (1, 2, 3...)
    private Integer totalShards;      // 总分片数
    private LocalDateTime publishTime; // 发布时间
    private List<Long> fanIds;        // 该分片的粉丝ID列表
}
