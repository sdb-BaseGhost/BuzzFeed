package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.sdb.buzzfeed.entity.Content;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface inboxMapper {
//    插入用户收件箱
    Boolean insertInbox(List<Long> activeFans, Long contentId, LocalDateTime publishTime);
//    下拉获取feed流
    List<Content> downFeed(Long userId, LocalDateTime lastTime, Integer num);

    List<Content> downFeedUp(Long userId, LocalDateTime lastTime, Long lastContentId, Integer num);
}
