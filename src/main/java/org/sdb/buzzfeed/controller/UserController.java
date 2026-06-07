package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    /**
     * 获取指定用户信息（公开资料）
     */
    @GetMapping("/{userId}")
    public Result getUser(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPasswordHash(null);
        return Result.success(user);
    }

    /**
     * 获取用户的关注列表
     */
    @GetMapping("/{userId}/following")
    public Result getFollowing(@PathVariable Long userId) {
        List<Long> followIds = userMapper.selectFollowsByUserId(userId);
        if (followIds == null || followIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<User> users = followIds.stream()
                .map(id -> {
                    User u = userMapper.selectById(id);
                    if (u != null) u.setPasswordHash(null);
                    return u;
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    /**
     * 获取用户的粉丝列表
     */
    @GetMapping("/{userId}/followers")
    public Result getFollowers(@PathVariable Long userId) {
        List<Long> fanIds = userMapper.selectFollowersByUserId(userId);
        if (fanIds == null || fanIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<User> users = fanIds.stream()
                .map(id -> {
                    User u = userMapper.selectById(id);
                    if (u != null) u.setPasswordHash(null);
                    return u;
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());
        return Result.success(users);
    }
}
