package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.mapper.inboxMapper;
import org.sdb.buzzfeed.mapper.postMapper;
import org.sdb.buzzfeed.mapper.userMapper;
import org.sdb.buzzfeed.service.PostService;
import org.sdb.buzzfeed.util.util;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final userMapper userMapper;
    private final postMapper postMapper;
    private final inboxMapper inboxMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Object postContent(Content content) {
        //1.拿到字段
        int id = content.getContentId();
        Long userId = content.getUserId();
        String shortText = content.getShortText();
        String longText = content.getLongText();
        String photo = content.getPhoto();
        String video = content.getVideo();
        String music = content.getMusic();
        String version = content.getVersion();
        String status = content.getStatus();
        var publishTime = content.getPublishTime();
        var updateTime = content.getUpdateTime();
        //2.判断是否需要送审
        //  根据userId去数据库查到粉丝量，判断是否超过阈值
        Integer followerNumber = userMapper.selectFollowsNumber(userId);
        //3.判断是否需要存储到云服务器
        //  这里模拟假地址
        if(video != null){
            //存到云端
            //返回一个地址
            System.out.println("模拟返回地址...");
        }
        //4.判断是否需要送审，异步发送到消息队列
        //5.返回用户发布成功，并根据消息队列的情况显示status
        //============接下来是消息队列的消费者做的事情=============
        //1.构建新的Content
        content.setStatus("发布中");
        //2.插入到用户的发件箱MySQL->item_info表
        postMapper.insertoutBox(content);
        List<Long> fansList = userMapper.selectFollowersByUserId(userId);
        Boolean isSuccess = false;
        //3.如果是大V的话
        if(followerNumber > 1){
            Map<String, String> mapContent = util.objectToMap(content);
            //  将发布内容存到Redis中，保证缓存预热和缓存一致性
            stringRedisTemplate.opsForHash().putAll((Long.toString(userId)),mapContent);
            //  遍历粉丝列表筛选出活跃粉丝
            List<Long> activeFans = userMapper.selectActiveFansByIds(fansList);
            //  批量插入到活跃粉丝的收件箱
            isSuccess = inboxMapper.insertInbox(activeFans, content.getContentId(),content.getPublishTime());

        }else{
            //4.不是大V的话就直接遍历粉丝列表
            //  推送到粉丝的收件箱中
            isSuccess = inboxMapper.insertInbox(fansList, content.getContentId(),content.getPublishTime());
        }
        return content;
    }
}

