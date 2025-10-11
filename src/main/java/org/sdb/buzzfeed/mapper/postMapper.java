package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.sdb.buzzfeed.entity.Content;

@Mapper
public interface postMapper {
    Boolean insertoutBox(Content content);
}
