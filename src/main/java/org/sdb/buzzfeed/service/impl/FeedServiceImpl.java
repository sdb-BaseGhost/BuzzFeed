package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.Feed;
import org.sdb.buzzfeed.mapper.contentMapper;
import org.sdb.buzzfeed.mapper.feedMapper;
import org.sdb.buzzfeed.mapper.inboxMapper;
import org.sdb.buzzfeed.mapper.userMapper;
import org.sdb.buzzfeed.service.FeedService;
import org.sdb.buzzfeed.utils.UserContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final feedMapper feedMapper;
    private final userMapper userMapper;
    private final contentMapper contentMapper;
    private final inboxMapper inboxMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public List<Content> upFeedM(Feed Feed) {

        return List.of();
    }

//    @Override
//    public List<Content> downFeedM(Feed downFeed) {
//        //1、拿到最后一条内容的时间和id
//        String lastId = downFeed.getContentId();
//        LocalDateTime lastTime = downFeed.getLastTime();
//        Long userId = UserContext.getUserId();
//        //获取用户的关注列表
//        List<Long> follows = userMapper.selectFollowsByUserId(userId);
//        //2、遍历关注列表
//        //2.1 找到需要用户去拉的关注者列表M个
//        //  拉取满足条件的各N条内容
//        //2.2 是推的话则直接从用户的收件箱找出满足条件的内容前N条内容
//        // 条件：发布时间小于当前时间，发布内容id大于当前内容id
//        for (int i = 0; i < follows.size(); i++) {
//            String contentId =
//        }
//        //3. 从这M+1个列表中归并选取N个合适的内容，即时间优先，时间相同则选择内容id更大的
//        //4. 查出内容后返回
//        return List.of();
//    }
    @Override
    public List<Content> downFeedM(Feed downFeed) {
        // 1. 拿到用户信息 & 翻页条件
        String lastId = downFeed.getContentId();
        LocalDateTime lastTime = downFeed.getLastTime();
        Long userId = UserContext.getUserId();
        int num = downFeed.getNum();

        // 2. 获取用户的关注列表
        List<Long> follows = userMapper.selectFollowsByUserId(userId);

        // 2.1 拆分关注者：活跃关注者 & 非活跃关注者
        List<Long> activeFollows = new ArrayList<>();
        List<Long> inactiveFollows = new ArrayList<>();
        for (Long followId : follows) {
            if (userMapper.isActive(followId)) {
                activeFollows.add(followId);
            } else {
                inactiveFollows.add(followId);
            }
        }

        // 2.2 拉取内容
        List<Content> candidateContents = new ArrayList<>();
        /*
        这个活跃不活跃是相对关注者来说的，遍历关注列表
        --下面查出来的就是该用户的关注列表
            select f.userId, f.fansNumber from follow f where f.userId in
                (select followId from follow where followerId = userId)
        --然后遍历过去，根据阈值放到两个列表：粉丝多moreList和粉丝少lessList；
            如果关注者粉丝少的话就是直接推--对应①sql
                ①select content_id,publish_time from inbox
                where (publish_time < ts or (publish_time = ts and content_id < last_content_id))
                    order by publish_time desc limit N
            如果关注者粉丝多的话则区分两种情况
                该粉丝是活跃粉丝，则是推到收件箱--对应①sql
                该粉丝是不活跃粉丝，则主动去拉取
                select content_id from item_info where creator_id = morelist.id
                and (publish_time < ts or (publish_time = ts and content_id < last_content_id))
                    order by publish_time desc limit N
         */
        // 3. 归并排序：按发布时间 desc，若时间相同按内容id desc(这也不对啊)多路归并还需要写
        //TODO 优先队列的多路归并

        // 4. 取前N条作为最终返回
        return candidateContents.stream()
                .limit(num)
                .collect(Collectors.toList());
        
    }

    @Override
    public Object downFeedR(Feed feed) {

        String lastId = feed.getContentId();
        LocalDateTime lastTime = feed.getLastTime();
        Long userId = UserContext.getUserId();
        int num = feed.getNum();

        String key = "inbox:" + userId;

        double maxScore;
        if (lastTime == null || lastId == null) {
            // 首次拉取，取当前时间为上界
            maxScore = Double.MAX_VALUE;
        } else {
            maxScore = buildScore(lastTime, lastId);
        }

        // ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
        // 按 score 倒序取 limit 条
        return redisTemplate.opsForZSet()
                .reverseRangeByScore(key, 0, maxScore, 0, num);
    }

    /**
     * 计算 Redis ZSET 的 score
     * 规则：score = 时间戳毫秒 * 1_000_000 + contentId
     */
    private long buildScore(LocalDateTime publishTime, String contentId) {
        long ts = publishTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        long Id = Long.parseLong(contentId);
        return ts * 1_000_000 + Id;
    }
}
