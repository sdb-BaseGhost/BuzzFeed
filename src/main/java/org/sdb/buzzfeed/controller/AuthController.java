package org.sdb.buzzfeed.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sdb.buzzfeed.entity.Result;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.entity.dto.LoginDTO;
import org.sdb.buzzfeed.entity.dto.RegisterDTO;
import org.sdb.buzzfeed.entity.vo.LoginVO;
import org.sdb.buzzfeed.entity.vo.RegisterVO;
import org.sdb.buzzfeed.service.AuthService;
import org.sdb.buzzfeed.utils.UserContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterDTO dto) {
        RegisterVO data = authService.register(dto);
        return Result.success("注册成功", data);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDTO dto) {
        LoginVO data = authService.login(dto);
        return Result.success("登录成功", data);
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
        User user = authService.getCurrentUser(userId);
        return Result.success(user);
    }
}
