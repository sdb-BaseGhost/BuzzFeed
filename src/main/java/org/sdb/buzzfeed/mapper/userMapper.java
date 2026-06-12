package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.*;
import org.sdb.buzzfeed.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {

    List<Long> selectActiveFansByIds(List<Long> fansList);

    Integer isActive(Long userId);

    Integer selectFollowsNumber(Long userId);

    List<Long> selectVbyId(@Param("follows") List<Long> follows);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM user WHERE user_id = #{userId}")
    User selectById(@Param("userId") Long userId);

    @Insert("INSERT INTO user (username, password_hash, email, display_name, is_active, follower_number, follows_number, post_count) " +
            "VALUES (#{username}, #{passwordHash}, #{email}, #{displayName}, #{isActive}, #{followerNumber}, #{followsNumber}, #{postCount})")
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "user_id")
    int insert(User user);

    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR display_name LIKE CONCAT('%', #{keyword}, '%') LIMIT 20")
    List<User> searchByKeyword(@Param("keyword") String keyword);
}
