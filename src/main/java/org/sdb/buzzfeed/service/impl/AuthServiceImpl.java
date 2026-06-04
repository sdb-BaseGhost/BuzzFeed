package org.sdb.buzzfeed.service.impl;

import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.mapper.userMapper;
import org.sdb.buzzfeed.service.AuthService;
import org.sdb.buzzfeed.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private userMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> register(String username, String password, String email, String displayName) {
        // 校验用户名唯一
        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setDisplayName(displayName != null ? displayName : username);
        user.setIsActive(true);
        user.setFollowerNumber(0);
        user.setFollowsNumber(0);
        user.setPostCount(0);

        userMapper.insert(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        return result;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        // 查找用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查是否活跃
        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new RuntimeException("账号已被禁用");
        }

        // 生成 JWT
        String token = JwtUtil.generateToken(user.getUserId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("displayName", user.getDisplayName());
        result.put("avatar", user.getAvatar());
        return result;
    }

    @Override
    public User getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 清除敏感信息
        user.setPasswordHash(null);
        return user;
    }
}
