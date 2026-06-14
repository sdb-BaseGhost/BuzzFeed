package org.sdb.buzzfeed.utils;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.Content;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feed 流 Redis 操作封装
 * 封装 inbox/outbox ZSET 和 content Hash 的所有读写操作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFeedHelper {

    private final RedisTemplate<String, String> redisTemplate;

    public static final String INBOX_PREFIX   = "inbox:";
    public static final String OUTBOX_PREFIX  = "outbox:";
    public static final String CONTENT_PREFIX = "content:";
    public static final int MAX_INBOX_SIZE = 500;

    public static final String H_ITEM_ID       = "itemId";
    public static final String H_CREATOR_ID    = "creatorId";
    public static final String H_ITEM_TYPE     = "itemType";
    public static final String H_TITLE         = "title";
    public static final String H_SUMMARY       = "summary";
    public static final String H_PUBLISH_TIME  = "publishTime";
    public static final String H_STATUS        = "status";

    // ===================== 工具方法 =====================

    /**
     * 将 contentId 零补到 20 位，保证字典序等于数值序
     */
    public String padContentId(Long contentId) {
        return String.format("%020d", contentId);
    }

    /**
     * 从零补字符串还原 contentId
     */
    public Long unpadContentId(String padded) {
        return Long.parseLong(padded);
    }

    /**
     * LocalDateTime -> Unix 秒级时间戳（double，供 ZSET score 使用）
     */
    public double toScore(LocalDateTime publishTime) {
        if (publishTime == null) return 0;
        return (double) publishTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * Unix 秒级时间戳 -> LocalDateTime
     */
    public LocalDateTime fromScore(double score) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond((long) score), ZoneId.systemDefault());
    }

    // ===================== Inbox ZSET 操作 =====================

    /**
     * 向单个用户的收件箱写入一条内容，并裁剪到最大容量
     */
    public void addInboxItem(Long userId, Long contentId, LocalDateTime publishTime) {
        String key = INBOX_PREFIX + userId;
        redisTemplate.opsForZSet().add(key, padContentId(contentId), toScore(publishTime));
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_INBOX_SIZE + 1));
    }

    /**
     * 从单个用户的收件箱移除一条内容
     */
    public void removeInboxItem(Long userId, Long contentId) {
        redisTemplate.opsForZSet().remove(INBOX_PREFIX + userId, padContentId(contentId));
    }

    /**
     * Pipeline 批量写入收件箱：同一内容推送到多个用户的收件箱
     */
    public void batchAddInbox(List<Long> userIds, Long contentId, LocalDateTime publishTime) {
        if (userIds == null || userIds.isEmpty()) return;
        String member = padContentId(contentId);
        double score = toScore(publishTime);
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long userId : userIds) {
                String key = INBOX_PREFIX + userId;
                connection.zAdd(key.getBytes(), score, member.getBytes());
                connection.zRemRange(key.getBytes(), 0, -(MAX_INBOX_SIZE + 1));
            }
            return null;
        });
        log.debug("Pipeline 批量写入收件箱: contentId={}, 用户数={}", contentId, userIds.size());
    }

    /**
     * 游标分页读取收件箱
     */
    public List<ContentScoreTuple> getInboxPage(Long userId, Double cursorScore, Long cursorContentId, int num) {
        return getZsetPage(INBOX_PREFIX + userId, cursorScore, cursorContentId, num);
    }

    // ===================== Outbox ZSET 操作 =====================

    /**
     * 向创作者的发件箱写入一条内容
     */
    public void addOutboxItem(Long creatorId, Long contentId, LocalDateTime publishTime) {
        redisTemplate.opsForZSet().add(OUTBOX_PREFIX + creatorId, padContentId(contentId), toScore(publishTime));
    }

    /**
     * 从创作者的发件箱移除一条内容
     */
    public void removeOutboxItem(Long creatorId, Long contentId) {
        redisTemplate.opsForZSet().remove(OUTBOX_PREFIX + creatorId, padContentId(contentId));
    }

    /**
     * 游标分页读取发件箱
     */
    public List<ContentScoreTuple> getOutboxPage(Long creatorId, Double cursorScore, Long cursorContentId, int num) {
        return getZsetPage(OUTBOX_PREFIX + creatorId, cursorScore, cursorContentId, num);
    }

    // ===================== Content Hash 操作 =====================

    /**
     * 写入/更新一条内容的首页展示缓存
     */
    public void setContentHash(Content content) {
        if (content == null || content.getItemId() == null) return;
        String key = CONTENT_PREFIX + content.getItemId();
        Map<String, String> hash = new HashMap<>();
        hash.put(H_ITEM_ID, String.valueOf(content.getItemId()));
        if (content.getCreatorId() != null)    hash.put(H_CREATOR_ID, String.valueOf(content.getCreatorId()));
        if (content.getItemType() != null)     hash.put(H_ITEM_TYPE, String.valueOf(content.getItemType()));
        if (content.getTitle() != null)        hash.put(H_TITLE, content.getTitle());
        if (content.getSummary() != null)      hash.put(H_SUMMARY, content.getSummary());
        if (content.getPublishTime() != null)  hash.put(H_PUBLISH_TIME, String.valueOf(toScore(content.getPublishTime())));
        if (content.getStatus() != null)       hash.put(H_STATUS, String.valueOf(content.getStatus()));
        redisTemplate.opsForHash().putAll(key, hash);
    }

    /**
     * 读取单条内容的首页展示缓存
     */
    public Content getContentHash(Long contentId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(CONTENT_PREFIX + contentId);
        if (entries == null || entries.isEmpty()) return null;
        return hashToContent(entries);
    }

    /**
     * Pipeline 批量读取多条内容的首页展示缓存
     */
    public Map<Long, Content> batchGetContentHash(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return Collections.emptyMap();
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long id : contentIds) {
                connection.hGetAll((CONTENT_PREFIX + id).getBytes());
            }
            return null;
        });
        Map<Long, Content> map = new LinkedHashMap<>();
        for (int i = 0; i < contentIds.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<String, String> stringHash = (Map<String, String>) results.get(i);
            if (stringHash != null && !stringHash.isEmpty()) {
                Content content = hashToContent(stringHash);
                if (content != null) map.put(contentIds.get(i), content);
            }
        }
        return map;
    }

    /**
     * 删除一条内容的首页展示缓存
     */
    public void deleteContentHash(Long contentId) {
        redisTemplate.delete(CONTENT_PREFIX + contentId);
    }

    // ===================== ZSET 游标分页核心实现 =====================

    /**
     * ZSET 游标分页通用实现（inbox/outbox 共用）
     *
     * 排序规则：score 降序（时间近->远），score 相同时 member 字典序升序（contentId 小->大）
     * 这与数据库联合索引 (publish_time DESC, content_id DESC) 的效果等价。
     *
     * 首次加载：ZREVRANGE 取前 num 条。
     * 加载更多：先取同 score 中 member &lt; cursorMember 的部分，
     *           再取严格更旧的 score 部分，凑满 num 条。
     */
    private List<ContentScoreTuple> getZsetPage(String key, Double cursorScore, Long cursorContentId, int num) {
        // 首次加载
        if (cursorScore == null || cursorContentId == null) {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, num - 1);
            if (tuples == null || tuples.isEmpty()) return Collections.emptyList();
            return tuples.stream()
                    .map(t -> new ContentScoreTuple(unpadContentId(t.getValue()), t.getScore()))
                    .collect(Collectors.toList());
        }

        String cursorMember = padContentId(cursorContentId);
        List<ContentScoreTuple> result = new ArrayList<>();

        // Step 1: 同 score 区间，member 字典序 &lt; cursorMember 的部分
        // ZREVRANGEBYSCORE key cursorScore cursorScore -> 同 score 全部，按 member 字典序倒序
        Set<ZSetOperations.TypedTuple<String>> sameScoreTuples =
                redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, cursorScore, cursorScore);
        if (sameScoreTuples != null) {
            for (ZSetOperations.TypedTuple<String> t : sameScoreTuples) {
                if (t.getValue().compareTo(cursorMember) < 0) {
                    result.add(new ContentScoreTuple(unpadContentId(t.getValue()), t.getScore()));
                    if (result.size() >= num) return result;
                }
            }
        }

        // Step 2: 严格更旧的 score 区间
        // (cursorScore 表示开区间：score &lt; cursorScore
        // Spring Data Redis 只支持闭区间，用 cursorScore - 0.001 模拟
        int remaining = num - result.size();
        if (remaining <= 0) return result;
        double exclusiveMax = cursorScore - 0.001;
        Set<ZSetOperations.TypedTuple<String>> olderTuples =
                redisTemplate.opsForZSet().reverseRangeByScoreWithScores(
                        key, Double.NEGATIVE_INFINITY, exclusiveMax, 0, remaining);
        if (olderTuples != null) {
            for (ZSetOperations.TypedTuple<String> t : olderTuples) {
                result.add(new ContentScoreTuple(unpadContentId(t.getValue()), t.getScore()));
            }
        }
        return result;
    }

    /**
     * Redis Hash -> Content 对象（仅含首页展示字段）
     */
    private Content hashToContent(Map<?, ?> hash) {
        Content content = new Content();
        try {
            String itemId = (String) hash.get(H_ITEM_ID);
            if (itemId == null) return null;
            content.setItemId(Long.parseLong(itemId));
            String creatorId = (String) hash.get(H_CREATOR_ID);
            if (creatorId != null) content.setCreatorId(Long.parseLong(creatorId));
            String itemType = (String) hash.get(H_ITEM_TYPE);
            if (itemType != null) content.setItemType(Integer.parseInt(itemType));
            content.setTitle((String) hash.get(H_TITLE));
            content.setSummary((String) hash.get(H_SUMMARY));
            String publishTime = (String) hash.get(H_PUBLISH_TIME);
            if (publishTime != null) content.setPublishTime(fromScore(Double.parseDouble(publishTime)));
            String status = (String) hash.get(H_STATUS);
            if (status != null) content.setStatus(Integer.parseInt(status));
        } catch (NumberFormatException e) {
            log.warn("content Hash 解析失败: {}", hash, e);
            return null;
        }
        return content;
    }

    /**
     * ZSET 分页返回的元组：contentId + score（发布时间戳）
     */
    @Data
    public static class ContentScoreTuple {
        private final Long contentId;
        private final double score;
        public LocalDateTime getPublishTime() {
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond((long) score), ZoneId.systemDefault());
        }
    }
}