package org.sdb.buzzfeed.service;

import org.sdb.buzzfeed.entity.User;
import org.sdb.buzzfeed.entity.dto.LoginDTO;
import org.sdb.buzzfeed.entity.dto.RegisterDTO;
import org.sdb.buzzfeed.entity.vo.LoginVO;
import org.sdb.buzzfeed.entity.vo.RegisterVO;

public interface AuthService {

    /**
     * 用户注册
     * @return 注册结果
     */
    RegisterVO register(RegisterDTO dto);

    /**
     * 用户登录
     * @return 登录结果（包含 token）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户的完整信息
     */
    User getCurrentUser(Long userId);
}
