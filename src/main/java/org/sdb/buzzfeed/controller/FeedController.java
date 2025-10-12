package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.Feed;
import org.sdb.buzzfeed.service.FeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * 上滑操作
     */
    @PostMapping("/upFeedM")
    public Result upFeedM(@RequestBody Feed feed){
        return Result.success(feedService);
    }

    /**
     * 下拉操作
     */
    @PostMapping("/downFeed")
    public Result downFeed(@RequestBody Feed feed){
        return Result.success(feedService.getFeed(feed));
    }

}
