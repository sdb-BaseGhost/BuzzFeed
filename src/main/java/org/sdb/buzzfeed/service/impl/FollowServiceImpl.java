package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.FollowRelation;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.mapper.FollowMapper;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.sdb.buzzfeed.service.FollowService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_FOLLOWING = 2000;
    private static final int DEFAULT_SIZE = 20;

    @Override
    @Transactional
    public void follow(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能关注自己");
        }
        Long followingCount = redisTemplate.opsForZSet().zCard(followingKey(fromUserId));
        if (followingCount != null && followingCount >= MAX_FOLLOWING) {
            throw new RuntimeException("关注数已达上限" + MAX_FOLLOWING);
        }
        long now = Instant.now().toEpochMilli();
        FollowRelation existing = followMapper.selectFollowing(fromUserId, toUserId);
        if (existing == null) {
            followMapper.insertFollowing(fromUserId, toUserId);
            followMapper.insertFollower(fromUserId, toUserId);
        } else if (existing.getType() == 2) {
            followMapper.updateFollowingType(fromUserId, toUserId, 1);
            followMapper.updateFollowerType(fromUserId, toUserId, 1);
        } else {
            return;
        }
        redisTemplate.opsForZSet().add(followingKey(fromUserId), String.valueOf(toUserId), now);
        redisTemplate.opsForZSet().add(followersKey(toUserId), String.valueOf(fromUserId), now);
    }

    @Override
    @Transactional
    public void unfollow(Long fromUserId, Long toUserId) {
        FollowRelation existing = followMapper.selectFollowing(fromUserId, toUserId);
        if (existing == null || existing.getType() == 2) { return; }
        followMapper.updateFollowingType(fromUserId, toUserId, 2);
        followMapper.updateFollowerType(fromUserId, toUserId, 2);
        redisTemplate.opsForZSet().remove(followingKey(fromUserId), String.valueOf(toUserId));
        redisTemplate.opsForZSet().remove(followersKey(toUserId), String.valueOf(fromUserId));
    }

    @Override
    public Map<String, Object> getFollowingList(Long userId, int size) {
        if (size <= 0) size = DEFAULT_SIZE;
        String key = followingKey(userId);
        Set<String> members = redisTemplate.opsForZSet().reverseRange(key, 0L, (long) size);
        List<Long> userIds;
        boolean hasMore = false;
        if (members != null && !members.isEmpty()) {
            userIds = new ArrayList<>();
            for (String m : members) userIds.add(Long.parseLong(m));
            if (userIds.size() > size) { hasMore = true; userIds = userIds.subList(0, size); }
        } else {
            List<FollowRelation> list = followMapper.selectFollowingList(userId, 1, size + 1);
            if (list == null || list.isEmpty()) return buildResult(Collections.emptyList(), false);
            hasMore = list.size() > size;
            if (hasMore) list = list.subList(0, size);
            userIds = new ArrayList<>();
            for (FollowRelation r : list) userIds.add(r.getToUserId());
            long now = Instant.now().toEpochMilli();
            for (FollowRelation r : list) {
                redisTemplate.opsForZSet().add(key, String.valueOf(r.getToUserId()), now);
            }
        }
        return buildResult(buildUserList(userIds), hasMore);
    }

    @Override
    public Map<String, Object> getFollowerList(Long userId, int size, Long myId) {
        if (size <= 0) size = DEFAULT_SIZE;
        String key = followersKey(userId);
        Set<String> members = redisTemplate.opsForZSet().reverseRange(key, 0L, (long) size);
        List<Long> userIds;
        boolean hasMore = false;
        if (members != null && !members.isEmpty()) {
            userIds = new ArrayList<>();
            for (String m : members) userIds.add(Long.parseLong(m));
            if (userIds.size() > size) { hasMore = true; userIds = userIds.subList(0, size); }
        } else {
            List<FollowRelation> list = followMapper.selectFollowerList(userId, 1, size + 1);
            if (list == null || list.isEmpty()) return buildResult(Collections.emptyList(), false);
            hasMore = list.size() > size;
            if (hasMore) list = list.subList(0, size);
            userIds = new ArrayList<>();
            for (FollowRelation r : list) userIds.add(r.getFromUserId());
            long now = Instant.now().toEpochMilli();
            for (FollowRelation r : list) {
                redisTemplate.opsForZSet().add(key, String.valueOf(r.getFromUserId()), now);
            }
        }
        Map<Long, String> statusMap = batchGetFollowStatus(myId, userIds);
        List<Map<String, Object>> userList = new ArrayList<>();
        for (Long uid : userIds) {
            User user = userMapper.selectById(uid);
            if (user != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("userId", user.getUserId());
                map.put("nickname", user.getDisplayName());
                map.put("avatar", user.getAvatar());
                map.put("username", user.getUsername());
                map.put("followStatus", statusMap.getOrDefault(uid, "NOT_FOLLOWING"));
                userList.add(map);
            }
        }
        return buildResult(userList, hasMore);
    }

    @Override
    public String getFollowStatus(Long myId, Long targetId) {
        if (myId.equals(targetId)) return "NOT_FOLLOWING";
        Boolean iFollow = redisTemplate.opsForZSet().score(followingKey(myId), String.valueOf(targetId)) != null;
        Boolean heFollows = redisTemplate.opsForZSet().score(followingKey(targetId), String.valueOf(myId)) != null;
        if (Boolean.TRUE.equals(iFollow) && Boolean.TRUE.equals(heFollows)) return "MUTUAL_FOLLOW";
        if (Boolean.TRUE.equals(iFollow)) return "FOLLOWING";
        return "NOT_FOLLOWING";
    }

    @Override
    public Map<Long, String> batchGetFollowStatus(Long myId, List<Long> targetIds) {
        Map<Long, String> result = new HashMap<>();
        if (targetIds == null || targetIds.isEmpty() || myId == null) return result;
        String myKey = followingKey(myId);
        for (Long tid : targetIds) {
            if (myId.equals(tid)) { result.put(tid, "NOT_FOLLOWING"); continue; }
            Boolean iFollow = redisTemplate.opsForZSet().score(myKey, String.valueOf(tid)) != null;
            if (Boolean.TRUE.equals(iFollow)) {
                Boolean heFollows = redisTemplate.opsForZSet().score(followingKey(tid), String.valueOf(myId)) != null;
                result.put(tid, Boolean.TRUE.equals(heFollows) ? "MUTUAL_FOLLOW" : "FOLLOWING");
            } else {
                result.put(tid, "NOT_FOLLOWING");
            }
        }
        return result;
    }

    private String followingKey(Long userId) { return "following:" + userId; }
    private String followersKey(Long userId) { return "followers:" + userId; }

    private List<Map<String, Object>> buildUserList(List<Long> userIds) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Long uid : userIds) {
            User user = userMapper.selectById(uid);
            if (user != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("userId", user.getUserId());
                map.put("nickname", user.getDisplayName());
                map.put("avatar", user.getAvatar());
                map.put("username", user.getUsername());
                list.add(map);
            }
        }
        return list;
    }

    private Map<String, Object> buildResult(List<Map<String, Object>> list, boolean hasMore) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", list);
        r.put("hasMore", hasMore);
        return r;
    }
}
