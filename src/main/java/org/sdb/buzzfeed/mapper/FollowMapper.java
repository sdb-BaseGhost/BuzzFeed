package org.sdb.buzzfeed.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.sdb.buzzfeed.entity.FollowRelation;

import java.util.List;

@Mapper
public interface FollowMapper {

    /** 查询 following 表中的关系记录 */
    FollowRelation selectFollowing(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    /** 新增 following 记录 */
    int insertFollowing(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    /** 更新 following 表的关系状态 */
    int updateFollowingType(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId, @Param("type") Integer type);

    /** 新增 follower 记录 */
    int insertFollower(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    /** 更新 follower 表的关系状态 */
    int updateFollowerType(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId, @Param("type") Integer type);

    /** 查询关注列表（我关注了谁） */
    List<FollowRelation> selectFollowingList(@Param("fromUserId") Long fromUserId, @Param("type") Integer type, @Param("limit") Integer limit);

    /** 查询粉丝列表（谁关注了我） */
    List<FollowRelation> selectFollowerList(@Param("toUserId") Long toUserId, @Param("type") Integer type, @Param("limit") Integer limit);

    /** 查询 follower 表中的关系记录 */
    FollowRelation selectFollower(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    /** 查询我关注的所有用户ID（type=1 有效关注） */
    List<Long> selectFollowingUserIds(@Param("fromUserId") Long fromUserId);

    /** 查询我的所有粉丝用户ID（type=1 有效关注） */
    List<Long> selectFollowerUserIds(@Param("toUserId") Long toUserId);

    /** 查询我的粉丝数量（type=1 有效关注） */
    Long countFollowers(@Param("toUserId") Long toUserId);
}
