package org.sdb.buzzfeed.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.utils.RedisFeedHelper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 内容首页展示缓存服务
 * 封装 content:{id} Redis Hash 的业务层读写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCacheService {

    private final RedisFeedHelper redisFeedHelper;

    /**
     * 写入/更新一条内容的缓存
     */
    public void cacheContent(Content content) {
        redisFeedHelper.setContentHash(content);
        log.debug("内容缓存写入: contentId={}", content.getItemId());
    }

    /**
     * 批量写入/更新内容缓存
     */
    public void cacheContents(List<Content> contents) {
        if (contents == null || contents.isEmpty()) return;
        for (Content c : contents) {
            redisFeedHelper.setContentHash(c);
        }
        log.debug("批量内容缓存写入: {} 条", contents.size());
    }

    /**
     * 读取单条内容缓存
     *
     * @return Content 对象（仅含首页展示字段），未命中返回 null
     */
    public Content getContentFromCache(Long contentId) {
        return redisFeedHelper.getContentHash(contentId);
    }

    /**
     * 批量读取内容缓存
     *
     * @param ids 内容ID列表
     * @return itemId -> Content 映射（仅包含缓存命中的）
     */
    public Map<Long, Content> getContentsFromCache(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        return redisFeedHelper.batchGetContentHash(ids);
    }

    /**
     * 删除一条内容的缓存
     */
    public void evictContent(Long contentId) {
        redisFeedHelper.deleteContentHash(contentId);
        log.debug("内容缓存删除: contentId={}", contentId);
    }
}