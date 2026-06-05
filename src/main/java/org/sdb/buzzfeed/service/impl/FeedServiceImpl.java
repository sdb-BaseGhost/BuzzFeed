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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final userMapper userMapper;
    private final outboxMapper outboxMapper;
    private final inboxMapper inboxMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public List<Content> getFeed(Feed feed) {
        //1、拿到最后一条内容的时间和id
        Long lastContentId = feed.getContentId();
        LocalDateTime lastTime = feed.getLastTime();
        Long userId = feed.getUserId();
        Integer type = feed.getType();
        //活跃用户标识
        Integer isActive = userMapper.isActive(userId);
        // 2. 获取用户的关注列表
        List<Long> follows = userMapper.selectFollowsByUserId(userId);
        //筛选出大v，因为大v在推送时只推给活跃粉丝,所以要去大v的发件箱找
        List<Long> Influencer = userMapper.selectVbyId(follows);
        //存放大V发件箱拉来的feed流
        List<Content> feeds = new ArrayList<>();
        //如果是下拉操作
        if(type == 0){
            //需要的是时间比当前最新内容时间还要大的内容
            //判断要去收件箱看还是去发件箱找
            if(isActive == 0){
                //活跃用户则直接去自己的收件箱找
                return inboxMapper.downFeed(userId, lastTime, 5);
            }else{
                //非活跃用户考虑的就多了
                // 2. 获取用户的关注列表
                //筛选出大v，因为大v在推送时只推给活跃粉丝,所以要去大v的发件箱找
                //TODO 去Redis中的发件箱拿
                //从大v的发件箱中获取feed流
                feeds = new ArrayList<>();
                if(Influencer.size() > 0){
                    feeds = outboxMapper.getContent(Influencer,5);
                }
                Map<Long, List<Content>> followMap = feeds.stream()
                        .collect(Collectors.groupingBy(Content::getCreatorId));
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
                    //删除展示的feed列表
                    followMap.remove(node.userId);
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
            if(isActive == 0){
                //活跃用户则直接去自己的收件箱找
                return inboxMapper.downFeedUp(userId, lastTime, lastContentId,5);
            }else{
                //非活跃用户考虑的就多了
                // 2. 获取用户的关注列表
                //筛选出大v，因为大v在推送时只推给活跃粉丝,所以要去大v的发件箱找
                //TODO 去Redis中的发件箱拿
                //从大v的发件箱中获取feed流
                if(Influencer.size() > 0){
                    feeds = outboxMapper.getContent(Influencer,5+1);
                }
                Map<Long, List<Content>> followMap = feeds.stream()
                        .collect(Collectors.groupingBy(Content::getCreatorId));
                //从自己的收件箱中获取feed流，因为普通关注者会直接推送到收件箱
                List<Content> contents = inboxMapper.downFeed(userId,lastTime,5+1);
                //多路归并
                //最小堆
                PriorityQueue<Node> pq = new PriorityQueue<>(((a, b) -> b.content.getPublishTime().compareTo(a.content.getPublishTime())));
                //把从发件箱拿到的m个的第一个都放进优先队列中
                for(Map.Entry<Long,List<Content>> entry : followMap.entrySet()){
                    List<Content> list = entry.getValue();
                    if(!list.isEmpty()){
                        //如果拿到的第N+1刚好是卡住的，就移除
                        Content firstContent = list.get(0);
                        //因为展示优先展示时间大的，内容id大的，所以内容id更大的就是已经读过的
                        if(firstContent.getPublishTime() == lastTime && firstContent.getItemId() >= lastContentId){
                            list.remove(0);
                        }
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
                    //删除已展示的在feed列表中的内容
                    followMap.remove(node.userId);
                    // 从该列表取下一条内容继续放入堆中
                    List<Content> fromList = node.userId == -1L ? contents : followMap.get(node.userId);
                    int nextIndex = node.index+1;
                    if(nextIndex < fromList.size()){
                        pq.offer(new Node(fromList.get(nextIndex), node.userId, nextIndex));
                    }
                }
                return result;
            }
        }
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
