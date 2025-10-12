package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.Feed;
import org.sdb.buzzfeed.mapper.outboxMapper;
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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final userMapper userMapper;
    private final outboxMapper outboxMapper;
    private final inboxMapper inboxMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public List<Content> upFeedM(Feed Feed) {

        return List.of();
    }

    @Override
    public List<Content> getFeed(Feed feed) {
        //1、拿到最后一条内容的时间和id
        String lastId = feed.getContentId();
        LocalDateTime lastTime = feed.getLastTime();
        Long userId = feed.getUserId();
        Integer type = feed.getType();
        //如果是下拉操作
        if(type == 0){
            //需要的是时间比当前最新内容时间还要大的内容
            //判断要去收件箱看还是去发件箱找
            Integer isActive = userMapper.isActive(userId);
            if(isActive == 0){
                //活跃用户则直接去自己的收件箱找
                return inboxMapper.downFeed(userId, lastTime, 5);
            }else{
                //非活跃用户考虑的就多了
                // 2. 获取用户的关注列表
                List<Long> follows = userMapper.selectFollowsByUserId(userId);
                //筛选出大v，因为大v在推送时只推给活跃粉丝,所以要去大v的发件箱找
                List<Long> Influencer = userMapper.selectVbyId(follows);
                //TODO 去Redis中的发件箱拿
                //从大v的发件箱中获取feed流
                List<Content> feeds = new ArrayList<>();
                if(Influencer.size() > 0){
                    feeds = outboxMapper.getContent(Influencer,5);
                }
                Map<Long, List<Content>> followMap = feeds.stream()
                        .collect(Collectors.groupingBy(Content::getUserId));
                //从自己的收件箱中获取feed流，因为普通关注者会直接推送到收件箱
                List<Content> contents = inboxMapper.downFeed(userId,lastTime,5);
                //多路归并
                //最小堆
                PriorityQueue<Node> pq = new PriorityQueue<>(((a, b) -> b.content.getPublishTime().compareTo(a.content.getPublishTime())));
                //把从发件箱拿到的m个的第一个都放进优先队列中
                for(Map.Entry<Long,List<Content>> entry : followMap.entrySet()){
                    List<Content> list = entry.getValue();
                    if(!list.isEmpty()){
                        pq.offer(new Node(list.get(0),entry.getKey(),0));
                    }
                }
                //把收件箱的第一个也放进队列中
                if(!contents.isEmpty()){
                    pq.offer(new Node(contents.get(0),-1L,0));
                }
                List<Content> result = new ArrayList<>();
                while(!pq.isEmpty() && result.size() < 5){
                    Node node = pq.poll();
                    result.add(node.content);
                    // 从该列表取下一条内容继续放入堆中
                    List<Content> fromList = node.userId == -1L ? contents : followMap.get(node.userId);
                    int nextIndex = node.index+1;
                    if(nextIndex < fromList.size()){
                        pq.offer(new Node(fromList.get(nextIndex), node.userId, nextIndex));
                    }
                }
                return result;
            }
        }else{
            //上拉操作
            //需要的是时间比当前最小的更新feed流时间还小的内容，需要时间和内容id
        }
        return List.of();
    }

    // 用于堆的辅助类
    static class Node {
        Content content;
        Long userId;
        int index;
        public Node(Content content, Long userId, int index) {
            this.content = content;
            this.userId = userId;
            this.index = index;
        }
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
//    @Override
//    public List<Content> downFeedM(Feed downFeed) {
//        // 1. 拿到用户信息 & 翻页条件
//        String lastId = downFeed.getContentId();
//        LocalDateTime lastTime = downFeed.getLastTime();
//        Long userId = UserContext.getUserId();
//        int num = downFeed.getNum();
//
//        // 2. 获取用户的关注列表
//        List<String> follows = userMapper.selectFollowsByUserId(userId);

        // 2.1 拆分关注者：活跃关注者 & 非活跃关注者
//        List<Long> activeFollows = new ArrayList<>();
//        List<Long> inactiveFollows = new ArrayList<>();
//        for (Long followId : follows) {
//            if (userMapper.isActive(followId)) {
//                activeFollows.add(followId);
//            } else {
//                inactiveFollows.add(followId);
//            }
//        }

        // 2.2 拉取内容
//        List<Content> candidateContents = new ArrayList<>();
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

//        // 4. 取前N条作为最终返回
//        return candidateContents.stream()
//                .limit(num)
//                .collect(Collectors.toList());
        
//    }

//    @Override
//    public Object downFeedR(Feed feed) {
//
//        String lastId = feed.getContentId();
//        LocalDateTime lastTime = feed.getLastTime();
//        Long userId = UserContext.getUserId();
//        int num = feed.getNum();
//
//        String key = "inbox:" + userId;
//
//        double maxScore;
//        if (lastTime == null || lastId == null) {
//            // 首次拉取，取当前时间为上界
//            maxScore = Double.MAX_VALUE;
//        } else {
//            maxScore = buildScore(lastTime, lastId);
//        }
//
//        // ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
//        // 按 score 倒序取 limit 条
//        return redisTemplate.opsForZSet()
//                .reverseRangeByScore(key, 0, maxScore, 0, num);
//    }



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
