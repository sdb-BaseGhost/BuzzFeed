package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface userMapper {
    List<Long> selectFollowsByUserId(Long userId);

    List<Long> selectFollowersByUserId(Long userId);

    List<Long> selectActiveFansByIds(List<Long> fansList);

    Integer isActive(Long userId);

    Integer selectFollowsNumber(Long userId);

    List<Long> selectVbyId(List<Long> follows);
}
