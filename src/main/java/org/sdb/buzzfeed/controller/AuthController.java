package org.sdb.buzzfeed.controller;

import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.service.AuthService;
import org.sdb.buzzfeed.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        String displayName = body.get("displayName");

        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        try {
            Map<String, Object> data = authService.register(username.trim(), password, email, displayName);
            return Result.success("注册成功", data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }

        try {
            Map<String, Object> data = authService.login(username.trim(), password);
            return Result.success("登录成功", data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登出（前端清 token 即可，后端仅返回成功）
     */
    @PostMapping("/logout")
    public Result logout() {
        return Result.success("已登出");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        try {
            User user = authService.getCurrentUser(userId);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
