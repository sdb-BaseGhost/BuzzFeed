package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.sdb.buzzfeed.entity.ContentVideo;

@Mapper
public interface ContentVideoMapper {
    void insert(ContentVideo video);
}
