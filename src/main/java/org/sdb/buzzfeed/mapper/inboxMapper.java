package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.sdb.buzzfeed.entity.Content;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface inboxMapper {
//    插入用户收件箱
    Boolean insertInbox(List<Long> activeFans, int contentId, LocalDateTime publishTime);
}
