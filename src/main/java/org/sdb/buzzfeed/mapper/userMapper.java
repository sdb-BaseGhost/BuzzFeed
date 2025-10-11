package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface userMapper {
    List<Long> selectFollowsByUserId(Long userId);

    List<Long> selectFollowersByUserId(String userId);

    List<Long> selectActiveFansByIds(List<Long> fansList);

    boolean isActive(Long followId);

    Integer selectFollowsNumber(String userId);
}
