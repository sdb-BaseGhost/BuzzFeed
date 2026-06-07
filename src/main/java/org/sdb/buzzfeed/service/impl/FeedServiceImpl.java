package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.Feed;
import org.sdb.buzzfeed.mapper.OutboxMapper;
import org.sdb.buzzfeed.mapper.InboxMapper;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.sdb.buzzfeed.service.FeedService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final UserMapper userMapper;
    private final OutboxMapper outboxMapper;
    private final InboxMapper inboxMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${feed.big-v-threshold:2}")
    private int bigVThreshold;

    private static final int DEFAULT_NUM = 5;
    /** 游标的最大时间，用于首次加载（获取最新内容） */
    private static final LocalDateTime MAX_TIME = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
    private static final long MAX_CONTENT_ID = Long.MAX_VALUE;

    @Override
    public List<Content> getFeed(Feed feed) {
        Long userId = feed.getUserId();
        Integer type = feed.getType();
        LocalDateTime lastTime = feed.getLastTime();
        Long lastContentId = feed.getContentId();
        int num = feed.getNum() > 0 ? feed.getNum() : DEFAULT_NUM;

        // 1. 获取关注列表
        List<Long> follows = userMapper.selectFollowsByUserId(userId);
        if (follows == null || follows.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 筛选大V
        List<Long> influencers = userMapper.selectVbyId(follows);
        boolean hasInfluencers = influencers != null && !influencers.isEmpty();

        // 3. 判断当前用户是否活跃（Redis key存在即为活跃）
        boolean isActive = Boolean.TRUE.equals(redisTemplate.hasKey("active:user:" + userId));

        // 4. 查收件箱
        List<Content> inboxList;
        if (type == 0) {
            // 下拉刷新: 获取更新的内容
            if (lastTime == null) {
                // 首次加载：用 downFeedUp 传入最大时间，获取最新N条
                inboxList = inboxMapper.downFeedUp(userId, MAX_TIME, MAX_CONTENT_ID, num);
            } else {
                // 下拉刷新：获取比 lastTime 更新的内容
                // downFeedUp 的 SQL 是 publish_time < lastTime，无法直接用于 >
                // 简化处理: 拉最新 num 条，内存过滤掉已有的
                List<Content> latest = inboxMapper.downFeedUp(userId, MAX_TIME, MAX_CONTENT_ID, num);
                inboxList = new ArrayList<>();
                for (Content c : latest) {
                    if (c.getPublishTime().isAfter(lastTime)) {
                        inboxList.add(c);
                    } else {
                        break; // 已按时间倒序，遇到旧的就停
                    }
                }
            }
        } else {
            // 上滑加载: 获取更旧的内容
            if (lastTime == null || lastContentId == null) {
                return Collections.emptyList();
            }
            inboxList = inboxMapper.downFeedUp(userId, lastTime, lastContentId, num);
        }

        // 5. 活跃用户或没有大V -> 直接返回收件箱结果
        if (isActive || !hasInfluencers) {
            return inboxList != null ? inboxList : Collections.emptyList();
        }

        // 6. 非活跃用户 + 有大V -> 拉大V发件箱 + 多路归并
        // 为每个大V分别查发件箱
        Map<Long, List<Content>> outboxMap = new LinkedHashMap<>();
        for (Long influencerId : influencers) {
            List<Content> outboxItems = outboxMapper.getContentByCreator(influencerId, num);
            if (outboxItems == null || outboxItems.isEmpty()) {
                continue;
            }
            // 上滑时过滤掉已展示过的内容
            if (type == 1 && lastTime != null && lastContentId != null) {
                outboxItems = filterOld(outboxItems, lastTime, lastContentId);
            }
            if (!outboxItems.isEmpty()) {
                outboxMap.put(influencerId, outboxItems);
            }
        }

        // 7. 多路归并
        if (inboxList == null) {
            inboxList = Collections.emptyList();
        }
        return mergeFeeds(inboxList, outboxMap, num);
    }

    /**
     * 过滤掉已展示过的旧内容（用于上滑场景）
     * 保留 publishTime > lastTime，或 publishTime == lastTime && itemId > lastContentId 的内容
     */
    private List<Content> filterOld(List<Content> list, LocalDateTime lastTime, Long lastContentId) {
        List<Content> result = new ArrayList<>();
        for (Content c : list) {
            int timeCmp = c.getPublishTime().compareTo(lastTime);
            if (timeCmp > 0) {
                result.add(c);
            } else if (timeCmp == 0 && c.getItemId() > lastContentId) {
                result.add(c);
            }
            // 发件箱按 publish_time DESC 排序，遇到更旧的后面都是更旧的
            if (timeCmp < 0) {
                break;
            }
        }
        return result;
    }

    /**
     * 多路归并：收件箱 + M个大V发件箱 -> Top N 条
     * 思路同 LeetCode 23: Merge k Sorted Lists
     */
    private List<Content> mergeFeeds(List<Content> inboxList, Map<Long, List<Content>> outboxMap, int num) {
        // 优先队列：按 publishTime 降序，相同时间按 itemId 降序
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> {
            int timeCmp = b.content.getPublishTime().compareTo(a.content.getPublishTime());
            if (timeCmp != 0) return timeCmp;
            return Long.compare(b.content.getItemId(), a.content.getItemId());
        });

        // 收件箱作为一路放入堆
        if (!inboxList.isEmpty()) {
            pq.offer(new Node(inboxList.get(0), -1L, 0));
        }

        // 每个大V的发件箱各作为一路放入堆
        for (Map.Entry<Long, List<Content>> entry : outboxMap.entrySet()) {
            List<Content> list = entry.getValue();
            if (!list.isEmpty()) {
                pq.offer(new Node(list.get(0), entry.getKey(), 0));
            }
        }

        // 归并取 Top N
        List<Content> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        while (!pq.isEmpty() && result.size() < num) {
            Node node = pq.poll();

            // 先推入该来源的下一条（不管当前节点是否重复，都要继续推进）
            List<Content> sourceList = node.sourceId == -1L ? inboxList : outboxMap.get(node.sourceId);
            int nextIndex = node.listIndex + 1;
            if (sourceList != null && nextIndex < sourceList.size()) {
                pq.offer(new Node(sourceList.get(nextIndex), node.sourceId, nextIndex));
            }

            // 去重：同一个 contentId 可能同时出现在收件箱和大V发件箱
            if (seenIds.contains(node.content.getItemId())) {
                continue;
            }
            seenIds.add(node.content.getItemId());
            result.add(node.content);
        }

        return result;
    }

    /**
     * 堆的辅助节点
     */
    static class Node {
        Content content;
        Long sourceId;   // -1=收件箱，其他=大V的creatorId
        int listIndex;   // 在该来源列表中的位置

        Node(Content content, Long sourceId, int listIndex) {
            this.content = content;
            this.sourceId = sourceId;
            this.listIndex = listIndex;
        }
    }
}
