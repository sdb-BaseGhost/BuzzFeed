package org.sdb.buzzfeed.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sdb.buzzfeed.entity.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            reject(response, "未登录，请先登录");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            if (JwtUtil.isTokenExpired(token)) {
                reject(response, "登录已过期，请重新登录");
                return false;
            }
            Long userId = JwtUtil.getUserId(token);
            String username = JwtUtil.getUsername(token);
            UserContext.set(userId, username);
            return true;
        } catch (Exception e) {
            reject(response, "无效的登录凭证");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear(); // 防止内存泄漏
    }

    private void reject(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(msg)));
    }
}
