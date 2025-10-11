package org.sdb.buzzfeed.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    // 你的秘钥（建议放到配置文件里）
    private static final String SECRET_KEY = "mySecretKey123!@#";

    // token 有效期（毫秒） - 24小时
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    /**
     * 生成 JWT
     * @param userId 用户ID
     * @return JWT字符串
     */
    public static String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 把 userId 放到 subject
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY) // 签名算法
                .compact();
    }

    /**
     * 解析 JWT，返回 Claims
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 token 中获取用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 判断 token 是否过期
     */
    public static boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().before(new Date());
    }
}
