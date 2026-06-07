package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.Content;

import java.util.List;

@Mapper
public interface OutboxMapper {

    //从发件箱中获取信息
    List<Content> getContent(List<Long> influencer, Integer num);

    //按单个创作者查发件箱（仅已发布的内容，拉模式用）
    List<Content> getContentByCreator(@Param("creatorId") Long creatorId, @Param("num") Integer num);
}
