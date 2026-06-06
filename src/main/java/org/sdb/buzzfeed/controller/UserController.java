package org.sdb.buzzfeed.controller;

import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

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
        // 清除敏感信息
        user.setPasswordHash(null);
        return Result.success(user);
    }
}
