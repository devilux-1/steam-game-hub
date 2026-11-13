package com.steamhub.util;

import com.steamhub.common.BizException;
import com.steamhub.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具，负责管理员令牌的签发与解析。
 * 使用 HS256 对称签名，密钥取自配置 jwt.secret，长度必须不少于 32 字节。
 */
@Component
public class JwtUtil {

    /** HMAC-SHA 密钥，由配置的字符串转换而来 */
    private final SecretKey secretKey;

    /** 令牌有效期（毫秒） */
    private final long expirationMs;

    /**
     * 构造 JWT 工具。
     *
     * @param secret       签名密钥字符串，非空且不少于 32 字节
     * @param expirationMs 令牌有效期毫秒数，必须为正数
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 为指定用户名签发访问令牌。
     *
     * @param username 管理员用户名，非空
     * @return 紧凑格式的 JWS 字符串
     */
    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析令牌并取出其中的用户名。
     *
     * @param token 紧凑格式的 JWS 字符串，非空
     * @return 令牌中的用户名
     * @throws BizException 令牌非法、被篡改或已过期时（错误码 401）
     */
    public String parseUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            // 过期、篡改、格式错误统一按未登录处理，不对外暴露具体失败原因
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
    }
}
