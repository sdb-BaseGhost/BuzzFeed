package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.ContentImage;
import org.sdb.buzzfeed.entity.ContentVideo;
import org.sdb.buzzfeed.entity.Feed;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.entity.vo.FeedItemVO;
import org.sdb.buzzfeed.mapper.ContentImageMapper;
import org.sdb.buzzfeed.mapper.ContentVideoMapper;
import org.sdb.buzzfeed.mapper.FollowMapper;
import org.sdb.buzzfeed.mapper.OutboxMapper;
import org.sdb.buzzfeed.mapper.InboxMapper;
import org.sdb.buzzfeed.mapper.PostMapper;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.sdb.buzzfeed.service.FeedService;
import org.sdb.buzzfeed.utils.RedisFeedHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final OutboxMapper outboxMapper;
    private final InboxMapper inboxMapper;
    private final PostMapper postMapper;
    private final ContentImageMapper contentImageMapper;
    private final ContentVideoMapper contentVideoMapper;
    private final RedisFeedHelper redisFeedHelper;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${minio.file-url-prefix:/files}")
    private String fileUrlPrefix;

    @Value("${feed.big-v-threshold:2}")
    private int bigVThreshold;

    private static final int DEFAULT_NUM = 5;
    /** 游标的最大时间，用于首次加载（获取最新内容） */
    private static final LocalDateTime MAX_TIME = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
    private static final long MAX_CONTENT_ID = Long.MAX_VALUE;

    @Override
    public List<FeedItemVO> getFeed(Feed feed) {
        Long userId = feed.getUserId();
        Integer type = feed.getType();
        LocalDateTime lastTime = feed.getLastTime();
        Long lastContentId = feed.getContentId();
        int num = feed.getNum() > 0 ? feed.getNum() : DEFAULT_NUM;

        // 1. 获取关注列表
        List<Long> follows = followMapper.selectFollowingUserIds(userId);
        if (follows == null || follows.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 筛选大V
        List<Long> influencers = userMapper.selectVbyId(follows);
        boolean hasInfluencers = influencers != null && !influencers.isEmpty();

        // 3. 判断当前用户是否活跃（Redis key存在即为活跃）
        boolean isActive = Boolean.TRUE.equals(redisTemplate.hasKey("active:user:" + userId));

        // 4. 查收件箱（Redis 优先，miss 回源 MySQL）
        List<Content> inboxList;
        if (type == 0) {
            // 下拉刷新: 获取最新内容
            if (lastTime == null) {
                // 首次加载
                inboxList = loadInboxFromRedisOrMysql(userId, null, null, num);
            } else {
                // 下拉刷新：获取比 lastTime 更新的内容
                // Redis: 全量拉最新 num 条，内存过滤掉已有的
                List<Content> latest = loadInboxFromRedisOrMysql(userId, null, null, num);
                inboxList = new ArrayList<>();
                for (Content c : latest) {
                    if (c.getPublishTime().isAfter(lastTime)) {
                        inboxList.add(c);
                    } else {
                        break;
                    }
                }
            }
        } else {
            // 上滑加载: 获取更旧的内容
            if (lastTime == null || lastContentId == null) {
                return Collections.emptyList();
            }
            inboxList = loadInboxFromRedisOrMysql(userId, lastTime, lastContentId, num);
        }

        // 5. 活跃用户或没有大V -> 直接返回收件箱结果
        if (isActive || !hasInfluencers) {
            return convertToVO(inboxList != null ? inboxList : Collections.emptyList());
        }

        // 6. 非活跃用户 + 有大V -> 拉大V发件箱 + 多路归并
        Map<Long, List<Content>> outboxMap = new LinkedHashMap<>();
        for (Long influencerId : influencers) {
            List<Content> outboxItems;
            if (type == 1 && lastTime != null && lastContentId != null) {
                // 上滑：用游标分页（Redis 自带游标过滤，无需手动 filterOld）
                outboxItems = loadOutboxFromRedisOrMysql(influencerId, lastTime, lastContentId, num);
            } else {
                // 下拉/首次：拉最新 num 条
                outboxItems = loadOutboxFromRedisOrMysql(influencerId, null, null, num);
            }
            if (outboxItems != null && !outboxItems.isEmpty()) {
                outboxMap.put(influencerId, outboxItems);
            }
        }

        // 7. 多路归并
        if (inboxList == null) {
            inboxList = Collections.emptyList();
        }
        return convertToVO(mergeFeeds(inboxList, outboxMap, num));
    }

    // ===================== Redis 优先读取 + MySQL 回源 =====================

    /**
     * 从收件箱加载内容（Redis ZSET 优先，miss 回源 MySQL inbox 表）
     */
    private List<Content> loadInboxFromRedisOrMysql(Long userId, LocalDateTime cursorTime, Long cursorContentId, int num) {
        Double cursorScore = (cursorTime != null) ? redisFeedHelper.toScore(cursorTime) : null;
        List<RedisFeedHelper.ContentScoreTuple> tuples = redisFeedHelper.getInboxPage(userId, cursorScore, cursorContentId, num);
        if (tuples != null && !tuples.isEmpty()) {
            List<Long> contentIds = tuples.stream()
                    .map(RedisFeedHelper.ContentScoreTuple::getContentId)
                    .collect(Collectors.toList());
            List<Content> contents = loadContentsFromRedisOrMysql(contentIds);
            if (!contents.isEmpty()) {
                return contents;
            }
        }
        // Redis miss -> 回源 MySQL
        if (cursorTime == null) {
            return inboxMapper.downFeedUp(userId, MAX_TIME, MAX_CONTENT_ID, num);
        }
        return inboxMapper.downFeedUp(userId, cursorTime, cursorContentId, num);
    }

    /**
     * 从发件箱加载内容（Redis ZSET 优先，miss 回源 MySQL item_info 表）
     */
    private List<Content> loadOutboxFromRedisOrMysql(Long creatorId, LocalDateTime cursorTime, Long cursorContentId, int num) {
        Double cursorScore = (cursorTime != null) ? redisFeedHelper.toScore(cursorTime) : null;
        List<RedisFeedHelper.ContentScoreTuple> tuples = redisFeedHelper.getOutboxPage(creatorId, cursorScore, cursorContentId, num);
        if (tuples != null && !tuples.isEmpty()) {
            List<Long> contentIds = tuples.stream()
                    .map(RedisFeedHelper.ContentScoreTuple::getContentId)
                    .collect(Collectors.toList());
            List<Content> contents = loadContentsFromRedisOrMysql(contentIds);
            if (!contents.isEmpty()) {
                return contents;
            }
        }
        // Redis miss -> 回源 MySQL item_info
        return outboxMapper.getContentByCreator(creatorId, num);
    }

    /**
     * 批量加载内容详情（Redis Hash 优先，miss 的部分回源 MySQL）
     */
    private List<Content> loadContentsFromRedisOrMysql(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return Collections.emptyList();

        // 先从 Redis Hash 批量取
        Map<Long, Content> cachedMap = redisFeedHelper.batchGetContentHash(contentIds);
        List<Content> result = new ArrayList<>(contentIds.size());

        // 收集 Redis miss 的 ID
        List<Long> missIds = new ArrayList<>();
        for (Long id : contentIds) {
            Content cached = cachedMap.get(id);
            if (cached != null) {
                result.add(cached);
            } else {
                missIds.add(id);
            }
        }

        // miss 的部分从 MySQL 按 ID 逐条查
        for (Long id : missIds) {
            Content dbContent = postMapper.selectById(id);
            if (dbContent != null) {
                result.add(dbContent);
            } else {
                log.debug("内容详情未命中 Redis 和 MySQL: contentId={}", id);
            }
        }

        return result;
    }

    /**
     * 将 Content 列表转换为 FeedItemVO 列表（批量查用户信息 + 媒体资源，避免 N+1）
     */
    private List<FeedItemVO> convertToVO(List<Content> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 收集所有 creatorId 并去重，批量查用户信息
        Set<Long> creatorIds = new LinkedHashSet<>();
        List<Long> imageItemIds = new ArrayList<>();
        List<Long> videoItemIds = new ArrayList<>();

        for (Content c : contentList) {
            if (c.getCreatorId() != null) {
                creatorIds.add(c.getCreatorId());
            }
            if (c.getItemType() != null) {
                if (c.getItemType() == 1) {
                    imageItemIds.add(c.getItemId());
                } else if (c.getItemType() == 2) {
                    videoItemIds.add(c.getItemId());
                }
            }
        }

        // 2. 批量查用户
        Map<Long, User> userMap = new HashMap<>();
        for (Long creatorId : creatorIds) {
            User user = userMapper.selectById(creatorId);
            if (user != null) {
                userMap.put(creatorId, user);
            }
        }

        // 3. 批量查图片，按 itemId 分组（拼接完整 URL）
        String urlPrefix = fileUrlPrefix + "/";
        Map<Long, List<String>> imageMap = new HashMap<>();
        if (!imageItemIds.isEmpty()) {
            List<ContentImage> images = contentImageMapper.selectByItemIds(imageItemIds);
            for (ContentImage img : images) {
                String imageUrl = img.getImageUri().startsWith("http")
                        ? img.getImageUri()
                        : urlPrefix + img.getImageUri();
                imageMap.computeIfAbsent(img.getItemId(), k -> new ArrayList<>())
                        .add(imageUrl);
            }
        }

        // 4. 批量查视频，按 itemId 映射
        Map<Long, ContentVideo> videoMap = new HashMap<>();
        if (!videoItemIds.isEmpty()) {
            List<ContentVideo> videos = contentVideoMapper.selectByItemIds(videoItemIds);
            for (ContentVideo v : videos) {
                videoMap.putIfAbsent(v.getItemId(), v);
            }
        }

        // 5. 组装 VO
        List<FeedItemVO> voList = new ArrayList<>(contentList.size());
        for (Content c : contentList) {
            FeedItemVO vo = new FeedItemVO();
            vo.setItemId(c.getItemId());
            vo.setCreatorId(c.getCreatorId());
            vo.setItemType(c.getItemType());
            vo.setTitle(c.getTitle());
            vo.setSummary(c.getSummary());
            vo.setPublishTime(c.getPublishTime());

            // 用户信息
            User user = userMap.get(c.getCreatorId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setDisplayName(user.getDisplayName());
                vo.setAvatar(user.getAvatar());
            }

            // 媒体资源
            List<String> urls = imageMap.get(c.getItemId());
            if (urls != null && !urls.isEmpty()) {
                vo.setImageUrls(urls);
            }

            ContentVideo video = videoMap.get(c.getItemId());
            if (video != null) {
                vo.setVideoUrl(video.getVideoUrl());
                // 封面 URL：存的是相对路径，需要拼接前缀
                if (video.getCoverUrl() != null && !video.getCoverUrl().isBlank()) {
                    String coverUrl = video.getCoverUrl().startsWith("http")
                            ? video.getCoverUrl()
                            : urlPrefix + video.getCoverUrl();
                    vo.setVideoCoverUrl(coverUrl);
                }
                vo.setVideoDuration(video.getDuration());
            }

            voList.add(vo);
        }
        return voList;
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
