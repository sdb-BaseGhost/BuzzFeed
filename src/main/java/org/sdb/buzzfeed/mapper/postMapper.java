package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.Content;

@Mapper
public interface postMapper {
    /** 获取下一个可用的 item_id（item_info.item_id 非自增） */
    Long getNextItemId();

    /** 插入内容主表 item_info */
    void insertContent(Content content);

    /** 更新内容状态（审核结果） */
    void updateState(@Param("itemId") Long itemId, @Param("status") Integer status);
}
