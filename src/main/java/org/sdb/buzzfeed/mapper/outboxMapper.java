package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.sdb.buzzfeed.entity.Content;

import java.util.List;

@Mapper
public interface OutboxMapper {

    //从发件箱中获取信息
    List<Content> getContent(List<Long> influencer, Integer num);
}
