package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.ContentVideo;

import java.util.List;

@Mapper
public interface ContentVideoMapper {
    void insert(ContentVideo video);

    /**
     * 根据 itemId 列表批量查询视频
     */
    List<ContentVideo> selectByItemIds(@Param("itemIds") List<Long> itemIds);
}
