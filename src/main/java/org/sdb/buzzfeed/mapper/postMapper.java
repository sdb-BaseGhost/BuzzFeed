package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.Content;

import java.util.List;

@Mapper
public interface PostMapper {
    /** 获取下一个可用的 item_id（item_info.item_id 非自增） */
    Long getNextItemId();

    /** 插入内容主表 item_info */
    void insertContent(Content content);

    /** 更新内容状态（审核结果） */
    void updateState(@Param("itemId") Long itemId, @Param("status") Integer status);

    /** 审核通过：更新状态 + 写入发布时间 */
    void approveContent(@Param("itemId") Long itemId, @Param("status") Integer status);

    /** 根据ID查询内容 */
    Content selectById(@Param("itemId") Long itemId);

    /** 查询指定用户发布的帖子（按发布时间倒序） */
    List<Content> selectByCreatorId(@Param("creatorId") Long creatorId, @Param("limit") int limit);
}
