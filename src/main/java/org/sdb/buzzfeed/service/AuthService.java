package org.sdb.buzzfeed.service;

import org.sdb.buzzfeed.entity.User;
import java.util.Map;

public interface AuthService {

    /**
     * 用户注册
     * @return 包含 userId 的 Map
     */
    Map<String, Object> register(String username, String password, String email, String displayName);

    /**
     * 用户登录
     * @return 包含 token 和用户信息的 Map
     */
    Map<String, Object> login(String username, String password);

    /**
     * 获取当前登录用户的完整信息
     */
    User getCurrentUser(Long userId);
}
