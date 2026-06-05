package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.ContentImage;
import java.util.List;

@Mapper
public interface ContentImageMapper {
    void batchInsert(@Param("list") List<ContentImage> images);
}
