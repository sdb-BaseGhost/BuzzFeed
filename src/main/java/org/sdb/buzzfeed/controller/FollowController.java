package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.service.FollowService;
import org.sdb.buzzfeed.utils.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * 关注用户
     */
    @PostMapping
    public Result follow(@RequestBody Map<String, Long> body) {
        Long toUserId = body.get("toUserId");
        if (toUserId == null) {
            return Result.error("参数错误：toUserId 不能为空");
        }
        Long myId = UserContext.getUserId();
        followService.follow(myId, toUserId);
        return Result.success();
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/{toUserId}")
    public Result unfollow(@PathVariable Long toUserId) {
        Long myId = UserContext.getUserId();
        followService.unfollow(myId, toUserId);
        return Result.success();
    }

    /**
     * 查询关注列表
     */
    @GetMapping("/following/{userId}")
    public Result getFollowingList(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getFollowingList(userId, size));
    }

    /**
     * 查询粉丝列表（附加"我是否关注了他"状态）
     */
    @GetMapping("/followers/{userId}")
    public Result getFollowerList(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "20") int size) {
        Long myId = UserContext.getUserId();
        return Result.success(followService.getFollowerList(userId, size, myId));
    }

    /**
     * 查询与目标用户的关注关系状态
     */
    @GetMapping("/status/{targetUserId}")
    public Result getFollowStatus(@PathVariable Long targetUserId) {
        Long myId = UserContext.getUserId();
        String status = followService.getFollowStatus(myId, targetUserId);
        return Result.success(Map.of("status", status));
    }
}
