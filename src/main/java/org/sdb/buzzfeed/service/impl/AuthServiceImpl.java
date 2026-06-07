package org.sdb.buzzfeed.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.entity.dto.LoginDTO;
import org.sdb.buzzfeed.entity.dto.RegisterDTO;
import org.sdb.buzzfeed.entity.vo.LoginVO;
import org.sdb.buzzfeed.entity.vo.RegisterVO;
import org.sdb.buzzfeed.mapper.UserMapper;
import org.sdb.buzzfeed.service.AuthService;
import org.sdb.buzzfeed.utils.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public RegisterVO register(RegisterDTO dto) {
        // 校验用户名唯一
        User existing = userMapper.selectByUsername(dto.getUsername().trim());
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setDisplayName(dto.getDisplayName() != null ? dto.getDisplayName().trim() : dto.getUsername().trim());
        user.setIsActive(true);
        user.setFollowerNumber(0);
        user.setFollowsNumber(0);
        user.setPostCount(0);

        userMapper.insert(user);

        return new RegisterVO(user.getUserId());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        String username = dto.getUsername().trim();

        // 查找用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查是否活跃
        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new RuntimeException("账号已被禁用");
        }

        // 登录成功，写入 Redis 活跃标记
        redisTemplate.opsForValue().set("active:user:" + user.getUserId(), "1", 7, TimeUnit.DAYS);

        // 生成 JWT
        String token = JwtUtil.generateToken(user.getUserId(), user.getUsername());

        return new LoginVO(token, user.getUserId(), user.getUsername(), user.getDisplayName(), user.getAvatar());
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
