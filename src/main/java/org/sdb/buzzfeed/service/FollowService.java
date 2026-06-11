package org.sdb.buzzfeed.service;

import org.sdb.buzzfeed.entity.Result;

import java.util.List;
import java.util.Map;

public interface FollowService {

    /**
     * 关注用户
     * @param fromUserId 关注者（当前登录用户）
     * @param toUserId   被关注者
     */
    void follow(Long fromUserId, Long toUserId);

    /**
     * 取消关注
     * @param fromUserId 当前登录用户
     * @param toUserId   被关注者
     */
    void unfollow(Long fromUserId, Long toUserId);

    /**
     * 查询关注列表（我关注了谁）
     * @return { list, hasMore }
     */
    Map<String, Object> getFollowingList(Long userId, int size);

    /**
     * 查询粉丝列表（谁关注了我），附加"我是否关注了他"状态
     * @param myId 当前登录用户，用于附加关注状态
     * @return { list, hasMore }
     */
    Map<String, Object> getFollowerList(Long userId, int size, Long myId);

    /**
     * 查询与目标用户的关注关系状态
     * @return FOLLOWING / NOT_FOLLOWING / MUTUAL_FOLLOW
     */
    String getFollowStatus(Long myId, Long targetId);

    /**
     * 批量查询与多个用户的关系状态
     */
    Map<Long, String> batchGetFollowStatus(Long myId, List<Long> targetIds);
}
